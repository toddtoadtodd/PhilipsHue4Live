

import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.SupportedFeature;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupClass;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupLightLocation;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupStream;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupType;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.ProxyMode;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Area;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Color;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Entertainment;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Message;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Observer;
import com.philips.lighting.hue.sdk.wrapper.entertainment.StartCallback;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Location;
import com.philips.lighting.hue.sdk.wrapper.entertainment.TweenType;
import com.philips.lighting.hue.sdk.wrapper.entertainment.animation.ConstantAnimation;
import com.philips.lighting.hue.sdk.wrapper.entertainment.animation.TweenAnimation;
import com.philips.lighting.hue.sdk.wrapper.entertainment.effect.AreaEffect;
import com.philips.lighting.hue.sdk.wrapper.entertainment.effect.ColorAnimationEffect;
import com.philips.lighting.hue.sdk.wrapper.entertainment.effect.ExplosionEffect;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;

import java.util.*;
import java.util.stream.Collectors;

public class HueConnectorEntertainment {
  static {
    // Load the huesdk native library before calling any SDK method
//    System.load("/Users/todd/Documents/code/PhilipsHue4Live/HueSDK/Apple/MacOS/Java/libhuesdk.dylib");
    System.load("C:\\Users\\toddm\\code\\PhilipsHue4Live\\HueSDK\\Windows\\huesdk.dll");

    String HUE_STORAGE_LOCATION = "C:\\Users\\toddm\\Music\\Ableton\\User Library\\Presets\\Instruments\\Max Instrument\\NewHueSdkMaxStorage";

    // Configure the storage location and log level for the Hue SDK
    Persistence.setStorageLocation(HUE_STORAGE_LOCATION, "Ableton");
    HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);
  }

  private static final String TAG = "HueQuickStartApp";

  private static final int MAX_HUE = 65535;

  public Bridge bridge;
  private Entertainment entertainment;
  private Group group;
  private boolean setupEntertainment;

  private BridgeDiscovery bridgeDiscovery;

  private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

  // UI elements

  enum UIState {
    Idle,
    BridgeDiscoveryRunning,
    BridgeDiscoveryResults,
    Connecting,
    Pushlinking,
    Connected,
    EntertainmentReady
  }

  // Directions for local: https://www.reddit.com/r/Hue/comments/65ui7k/how_can_i_connect_the_philips_hue_bridge_directly/
  public HueConnectorEntertainment() {
    this("169.254.8.173");
  }

  public HueConnectorEntertainment(String ipAddress, boolean setupEntertainment) {
    this.bridge = null;
    this.entertainment = null;
    this.setupEntertainment = setupEntertainment;

    // Local connection
    if (ipAddress == null) {
      startBridgeDiscovery();
    } else {
      connectToBridge(ipAddress);
    }
  }

  public HueConnectorEntertainment(String ipAddress) {
    this(ipAddress, true);
  }

  /**
   * Use the KnownBridges API to retrieve the last connected bridge
   * @return Ip address of the last connected bridge, or null
   */
  private String getLastUsedBridgeIp() {
    List<KnownBridge> bridges = KnownBridges.getAll();

    if (bridges.isEmpty()) {
      return null;
    }

    return Collections.max(bridges, new Comparator<KnownBridge>() {
      @Override
      public int compare(KnownBridge a, KnownBridge b) {
        return a.getLastConnected().compareTo(b.getLastConnected());
      }
    }).getIpAddress();
  }

  /**
   * Start the bridge discovery search
   * Read the documentation on meethue for an explanation of the bridge discovery options
   */
  private void startBridgeDiscovery() {
    disconnectFromBridge();

    bridgeDiscovery = new BridgeDiscoveryImpl();
    // ALL Include [UPNP, IPSCAN, NUPNP, MDNS] but in some nets UPNP, NUPNP and MDNS is not working properly
    // bridgeDiscovery.search(BridgeDiscovery.Option.ALL, bridgeDiscoveryCallback);
    bridgeDiscovery.search(bridgeDiscoveryCallback);
    updateUI(UIState.BridgeDiscoveryRunning, "Scanning the network for hue bridges...");
  }


  /**
   * Stops the bridge discovery if it is still running
   */
  private void stopBridgeDiscovery() {
    if (bridgeDiscovery != null) {
      bridgeDiscovery.stop();
      bridgeDiscovery = null;
    }
  }

  /**
   * The callback that receives the results of the bridge discovery
   */
  private BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
    @Override
    public void onFinished(final List<BridgeDiscoveryResult> results, final BridgeDiscovery.ReturnCode returnCode) {
      // Set to null to prevent stopBridgeDiscovery from stopping it
      bridgeDiscovery = null;
      if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
        bridgeDiscoveryResults = results;

        List<String> ips = results.stream().map(BridgeDiscoveryResult::getIp).collect(Collectors.toList());
        updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network, with IP's: " + ips + ".");
      } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
        System.out.println(TAG + " - Bridge discovery stopped.");
      } else {
        updateUI(UIState.Idle, "Error doing bridge discovery: " + returnCode);
      }
    }
  };

  /**
   * Use the BridgeBuilder to create a bridge instance and connect to it
   */
  public void connectToBridge(String bridgeIp) {
    stopBridgeDiscovery();
    disconnectFromBridge();

    bridge = new BridgeBuilder("app name", "device name")
        .setIpAddress(bridgeIp)
        .setConnectionType(BridgeConnectionType.LOCAL)
        .setBridgeConnectionCallback(bridgeConnectionCallback)
        .addBridgeStateUpdatedCallback(createBridgeStateUpdatedCallback())
        .build();

    bridge.connect();

    updateUI(UIState.Connecting, "Connecting to bridge...");
  }

  /**
   * Disconnect a bridge
   * The hue SDK supports multiple bridge connections at the same time,
   * but for the purposes of this demo we only connect to one bridge at a time.
   */
  private void disconnectFromBridge() {
    if (bridge != null) {
      bridge.disconnect();
      bridge = null;
    }
  }

  /**
   * The callback that receives bridge connection events
   */
  private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback() {
    @Override
    public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent) {
      System.out.println(TAG + " - Connection event: " + connectionEvent);

      switch (connectionEvent) {
        case LINK_BUTTON_NOT_PRESSED:
          updateUI(UIState.Pushlinking, "Press the link button to authenticate.");
          break;

        case COULD_NOT_CONNECT:
          updateUI(UIState.Connecting, "Could not connect.");
          break;

        case CONNECTION_LOST:
          updateUI(UIState.Connecting, "Connection lost. Attempting to reconnect.");
          break;

        case CONNECTION_RESTORED:
          updateUI(UIState.Connected, "Connection restored.");
          break;

        case DISCONNECTED:
          // User-initiated disconnection.
          break;

        default:
          break;
      }
    }

    @Override
    public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list) {
      for (HueError error : list) {
        System.out.println(TAG + " - Connection error: " + error.toString());
      }
    }
  };

  /**
   * The callback the receives bridge state update events
   */
  private BridgeStateUpdatedCallback createBridgeStateUpdatedCallback() {
    final boolean setupEntertainment = this.setupEntertainment;
    return new BridgeStateUpdatedCallback() {
      @Override
      public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent) {
        System.out.println(TAG + " - Bridge state updated event: " + bridgeStateUpdatedEvent);

        switch (bridgeStateUpdatedEvent) {
          case INITIALIZED:
            // The bridge state was fully initialized for the first time.
            // It is now safe to perform operations on the bridge state.
            updateUI(UIState.Connected, "Connected!");
            if (setupEntertainment) {
              setupEntertainment();
            }
            break;

          case LIGHTS_AND_GROUPS:
            // At least one light was updated.
            break;

          default:
            break;
        }
      }
    };
  }

//  /**
//   * Randomize the color of all lights in the bridge
//   * The SDK contains an internal processing queue that automatically throttles
//   * the rate of requests sent to the bridge, therefore it is safe to
//   * perform all light operations at once, even if there are dozens of lights.
//   */
//  private void randomizeLights() {
//    BridgeState bridgeState = bridge.getBridgeState();
//    List<LightPoint> lights = bridgeState.getLights();
//
//    Random rand = new Random();
//
//    for (final LightPoint light : lights) {
//      final LightState lightState = new LightState();
//
//      lightState.setHue(rand.nextInt(MAX_HUE));
//
//      light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
//        @Override
//        public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
//          if (returnCode == ReturnCode.SUCCESS) {
//            System.out.println(TAG + " - Changed hue of light " + light.getIdentifier() + " to " + lightState.getHue());
//          } else {
//            System.out.println(TAG + " - Error changing hue of light " + light.getIdentifier());
//            for (HueError error : errorList) {
//              System.out.println(TAG + " - " + error.toString());
//            }
//          }
//        }
//      });
//    }
//  }

  /**
   * Refresh the username in case it was created before entertainment was available
   */
  private void setupEntertainment() {
    updateUI(null, "MADE IT HERE A");
    setupEntertainmentGroup();
    // Having this in the callback broke everything ??
//    bridge.refreshUsername(new BridgeResponseCallback() {
//      @Override
//      public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors) {
//        if (returnCode == ReturnCode.SUCCESS) {
//          setupEntertainmentGroup();
//        } else {
//          // ...
//        }
//      }
//    });
  }

  /**
   * Setup the group used for entertainment
   */
  private void setupEntertainmentGroup() {
    // look for an existing entertainment group

    List<Group> groups = bridge.getBridgeState().getGroups();
    updateUI(null, "THIS MANY GROUPS FOUND: " + groups.size());
    for (Group group : groups) {
      updateUI(null, "THIS IS THE GROUP NAME -- " + group.getName());
      if (group.getGroupType() == GroupType.ENTERTAINMENT) {
        updateUI(null, "THIS IS THE ENTERTAINMENT GROUP NAME -- " + group.getName());
        this.group = group;
        createEntertainmentObject(group.getIdentifier());
        return;
      }
    }

    // Could not find an existing group, create a new one with all color lights

    List<LightPoint> validLights = getValidLights();

    if (validLights.isEmpty()) {
      System.out.println(TAG + " - No color lights found for entertainment");
      return;
    }

    createEntertainmentGroup(validLights);
  }

  /**
   * Create an entertainment group
   * @param validLights List of supported lights
   */
  private void createEntertainmentGroup(List<LightPoint> validLights) {
    ArrayList<String> lightIds = new ArrayList<String>();
    ArrayList<GroupLightLocation> lightLocations = new ArrayList<GroupLightLocation>();
    Random rand = new Random();

    for (LightPoint light : validLights) {
      lightIds.add(light.getIdentifier());

      GroupLightLocation location = new GroupLightLocation();
      location.setLightIdentifier(light.getIdentifier());
      location.setX(rand.nextInt(11) / 10.0 - 0.5);
      location.setY(rand.nextInt(11) / 10.0 - 0.5);
      location.setZ(rand.nextInt(11) / 10.0 - 0.5);

      lightLocations.add(location);
    }

    Group group = new Group();
    group.setName("NewEntertainmentGroup");
    group.setGroupType(GroupType.ENTERTAINMENT);
    group.setGroupClass(GroupClass.TV);

    group.setLightIds(lightIds);
    group.setLightLocations(lightLocations);

    GroupStream stream = new GroupStream();
    stream.setProxyMode(ProxyMode.AUTO);
    group.setStream(stream);
    this.group = group;

    bridge.createResource(group, new BridgeResponseCallback() {
      @Override
      public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors) {
        if (returnCode == ReturnCode.SUCCESS) {
          createEntertainmentObject(responses.get(0).getStringValue());
        } else {
          System.out.println(TAG + " - Could not create entertainment group.");
        }
      }
    });
  }

  /**
   * Create an entertainment object and register an observer to receive messages
   * @param groupId The entertainment group to be used
   */
  private void createEntertainmentObject(String groupId) {
    int defaultPort = 2100;

    entertainment = new Entertainment(bridge, defaultPort, groupId);

    entertainment.registerObserver(new Observer() {
      @Override
      public void onMessage(Message message) {
        //System.out.println(TAG + " - Entertainment message: " + message.getType() + " " + message.getUserMessage());
      }
    }, Message.Type.RENDER);

    updateUI(UIState.EntertainmentReady, "Connected, entertainment ready.");
    entertainment.start(new StartCallback() {
      @Override
      public void handleCallback(StartStatus status) {
        if (status != StartStatus.Success) {
          return;
        }
        //playExplosionEffect();
      }
    });
  }

  /**
   * Get a list of all lights that support entertainment
   * @return Valid lights
   */
  private List<LightPoint> getValidLights() {
    ArrayList<LightPoint> validLights = new ArrayList<LightPoint>();
    for (final LightPoint light : bridge.getBridgeState().getLights()) {
      if (light.getInfo().getSupportedFeatures().contains(SupportedFeature.STREAM_PROXYING)) {
        validLights.add(light);
      }
    }
    return validLights;
  }

  /**
   * Add an explosion effect to the entertainment engine
   */
  public void playExplosionEffect(double time, double r, double b, double g, double brightness) {
    Color color = getColorFrom255RgbBrightness(r, g, b, brightness);
    Location center = new Location(0.0, 0.0);
    final double duration = time * 1000;
    double radius = 1.7;
    double radiusExpansionTime = 100;
    double intensityExpansionTime = 50;

    ExplosionEffect effect = new ExplosionEffect();
    effect.prepareEffect(color, center, duration, radius, radiusExpansionTime, intensityExpansionTime);
    effect.setFixedOpacity(1.0);
    effect.enable();

    entertainment.lockMixer();
    entertainment.addEffect(effect);
    entertainment.unlockMixer();
  }

  public void colorAll() {
    double r = Math.random() * 255;
    double g = Math.random() * 255;
    double b = Math.random() * 255;
    double bri = Math.random() * 255;

    setColorForArea(-1, 1, 1, -1, r, g, b, bri);
  }

  public void setColorForArea(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY,
                              double r, double g, double b, double brightness) {
    AreaEffect effect = new AreaEffect();
    effect.addArea(new Area(topLeftX, topLeftY, bottomRightX, bottomRightY, "Test", false));
    Color c = getColorFrom255RgbBrightness(r, g, b, brightness);
    effect.setFixedColor(c);
    // effect.setFixedOpacity(1.0);
    effect.enable();

      entertainment.lockMixer();
      entertainment.addEffect(effect);
      entertainment.unlockMixer();
  }

//  public void draw(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY,
//                   double transitionTime, double toR, double toG, double toB) {
//    // TODO: This starts the light at the current state of a random bulb in the area
//
//    Color fromRGB = null;
//    for (GroupLightLocation l : group.getLightLocations()) {
//      if (l.getX() >= topLeftX && l.getX() <= bottomRightX && l.getY() <= topLeftY && l.getY() >= bottomRightY) {
//        System.out.println("TEST " + l.getLightIdentifier());
//        LightPoint light = bridge.getBridgeState().getLight(l.getLightIdentifier());
//        HueColor.RGB rgb = light.getLightState().getColor().getRGB();
//        fromRGB = normalizeRGB(rgb.r, rgb.b, rgb.g);
//        System.out.println(String.format("Fetched color: r: %d, g: %d, b: %d", (int) fromRGB.getRed(), (int) fromRGB.getGreen(), (int) fromRGB.getBlue()));
//        break;
//      }
//    }
//    AreaEffect effect = new AreaEffect();
//
//    effect.addArea(new Area(-1, 1, 1, -1, "Test", false));
//    effect.setColorAnimation(
//        new TweenAnimation(fromRGB.getRed() / 255.0D, toR, transitionTime, TweenType.EaseInOutSine),
//        new TweenAnimation(fromRGB.getBlue() / 255.0D, toG, transitionTime, TweenType.EaseInOutSine),
//        new TweenAnimation(fromRGB.getGreen() / 255.0D, toB, transitionTime, TweenType.EaseInOutSine)
//    );
//
//    effect.enable();
//
//    AreaEffect fixedColor = new AreaEffect();
//    fixedColor.addArea(new Area(-1, 1, 1, -1, "Test2", false));
//    fixedColor.setFixedColor(new Color(toR, toG, toB));
//    fixedColor.enable();
//
//    entertainment.lockMixer();
//    entertainment.addEffect(effect);
//    entertainment.addEffect(fixedColor);
//    entertainment.unlockMixer();
//
//
//    entertainment.addEffect(effect);
//
//    entertainment.lockMixer();
//    entertainment.addEffect(effect);
//    entertainment.unlockMixer();
//
//  }

  public void strobeRandomLight(double holdTime, double r, double g, double b, double bri) {
    List<GroupLightLocation> locations = group.getLightLocations();
    GroupLightLocation randLocation = locations.get((int) (Math.random() * locations.size()));
    strobe(holdTime, randLocation.getX(), randLocation.getY(), randLocation.getX(), randLocation.getY(), r, g, b, bri);
  }

  public void strobe(double holdTime, double topLeftX, double topLeftY, double bottomRightX, double bottomRightY,
                     double r, double g, double b, double bri) {
    AreaEffect on = getAreaEffectForCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);
    Color color = getColorFrom255RgbBrightness(r, g, b, bri);
    on.setColorAnimation(
        new ConstantAnimation(color.getRed(), holdTime * 1000),
        new ConstantAnimation(color.getGreen(), holdTime * 1000),
        new ConstantAnimation(color.getBlue(), holdTime * 1000)
    );
    playEffect(on);
  }

  public void cooldownStrobeRandomLight(double holdTime, double r, double g, double b, double bri) {
    List<GroupLightLocation> locations = group.getLightLocations();
    GroupLightLocation randLocation = locations.get((int) (Math.random() * locations.size()));
    cooldownStrobe(holdTime, randLocation.getX(), randLocation.getY(), randLocation.getX(), randLocation.getY(), r, g, b, bri);
  }

  public void cooldownStrobe(double holdTime, double topLeftX, double topLeftY, double bottomRightX, double bottomRightY,
                     double r, double g, double b, double bri) {
    AreaEffect on = getAreaEffectForCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);
    Color color = getColorFrom255RgbBrightness(r, g, b, bri);

    on.setColorAnimation(
        new TweenAnimation(color.getRed(), 0,holdTime * 1000, TweenType.EaseInOutSine),
        new TweenAnimation(color.getGreen(), 0,holdTime * 1000, TweenType.EaseInOutSine),
        new TweenAnimation(color.getBlue(), 0,holdTime * 1000, TweenType.EaseInOutSine)
    );

    playEffect(on);
  }

  private static AreaEffect getAreaEffectForCoordinates(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY) {
    AreaEffect effect = new AreaEffect();
    effect.addArea(new Area(topLeftX, topLeftY, bottomRightX, bottomRightY, "Animation", false));
    return effect;
  }

  private void playEffect(ColorAnimationEffect effect) {
    effect.setFixedOpacity(1.0);
    effect.enable();
    entertainment.lockMixer();
    entertainment.addEffect(effect);
    entertainment.unlockMixer();
  }

  public static Color getColorFrom255RgbBrightness(double r, double g, double b, double brightness) {
    double brightnessCoefficient = brightness / 255.0;
    Color color = new Color(r, g, b);
    color.applyBrightness(brightnessCoefficient);
    return color;

//    System.out.println("red: " + r2 + ", green: " + g2 + ", blue: " + b2);
//    return new Color(r2, g2, b2);
  }

  public void turnAreaOff() {
    AreaEffect effect = new AreaEffect();
    effect.addArea(new Area(-1, 1, 1, -1, "Test", false));
    effect.setFixedColor(new Color(0, 0, 0));
    effect.enable();

    entertainment.lockMixer();
    entertainment.addEffect(effect);
    entertainment.unlockMixer();
  }

  public void draw(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY, int transitionTime, int brightness, double red, double green, double blue) {

    AreaEffect effect = new AreaEffect();
    effect.addArea(new Area(topLeftX, topLeftY, bottomRightX, bottomRightY, "Draw", false));

//    Animation r = new TweenAnimationz(blue, transitionTime, 10, TweenType.EaseInOutSine);

//    effect.setFixedColor(new Color(Math.random(), Math.random(), Math.random()));
//    Animation redAnimation = new ConstantAnimation(red, 0);
//    Animation greenAnimation = new ConstantAnimation(green, 0);
//    Animation blueAnimation = new ConstantAnimation(blue, 0);
//    effect.setColorAnimation(redAnimation, greenAnimation, blueAnimation);
    // effect.setFixedOpacity(0.25);
    //effect.setColorAnimation(r, g, b);

    effect.setColorAnimation(new TweenAnimation(1, 0,1000,TweenType.Linear),new TweenAnimation(0, 1,1000,TweenType.Linear),new ConstantAnimation(1));

    effect.enable();

    entertainment.lockMixer();
    entertainment.addEffect(effect);
    entertainment.unlockMixer();
  }

  private void updateUI(final UIState state, final String status) {
    System.out.println(TAG + " - Status: " + status);
  }

}
