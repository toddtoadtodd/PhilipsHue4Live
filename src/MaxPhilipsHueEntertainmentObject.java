import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cycling74.max.Atom;
import com.cycling74.max.DataTypes;
import com.cycling74.max.MaxObject;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MaxPhilipsHueEntertainmentObject extends MaxObject {
  public static final String HUE_STORAGE_LOCATION = "/Users/tmaegerle/Music/Ableton/User Library/Presets/Instruments/Max Instrument/NewHueSdkMaxStorage";



  private HueConnectorEntertainment hue;

  // inlets
  Object[] inletVals;
  private enum Inlet {
    DUMMY,
    TOP_LEFT_X,
    TOP_LEFT_Y,
    BOTTOM_RIGHT_X,
    BOTTOM_RIGHT_Y,
    BRIGHTNESS,
    RED,
    GREEN,
    BLUE,
    RENDER,
    ANIMATION_TOP_LEFT_X,
    ANIMATION_TOP_LEFT_Y,
    ANIMATION_BOTTOM_RIGHT_X,
    ANIMATION_BOTTOM_RIGHT_Y,
    ANIMATION_TIME,
    ANIMATION_TYPE,
    ANIMATION_BRIGHTNESS,
    ANIMATION_AFFECT_SINGLE_RANDOM_LIGHT,
    ANIMATION_RED,
    ANIMATION_GREEN,
    ANIMATION_BLUE,
    RESET,
    BANG_SHOULD_RESET,
    BRIDGE_IP,
  }

  private static final int[] INLET_TYPES = new int[] {
      DataTypes.INT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.FLOAT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.INT,
      DataTypes.MESSAGE,
  };

  private enum AnimationType {
    STROBE,
    COOLDOWN_STROBE
  }

  private boolean lightsNeedRerender;

  private static final ScheduledExecutorService RENDER_THREAD_POOL = Executors.newScheduledThreadPool(5);
  private static final Executor FX_RENDER_THREAD_POOL = Executors.newFixedThreadPool(5);

  private static final int RENDER_INIT_DELAY_MS = 5000;
  private static final int RENDER_PERIOD_MS = 38;

  public MaxPhilipsHueEntertainmentObject() {


//    hue = new HueConnectorEntertainment();
    inletVals = new Object[INLET_TYPES.length];
    // Set default values
    for (int i = 0; i < INLET_TYPES.length; i++)
      inletVals[i] = 0;

    declareInlets(INLET_TYPES);
    declareOutlets(NO_OUTLETS);
    lightsNeedRerender = true;
  }

//  public void inlet(int input) {
//    int inletIndex = getInlet();
//    System.out.println("B: " + input);
//
//    inletVals[inletIndex] = input;
//  }

//  public void inlet(float input) {
//    int inletIndex = getInlet();
//    updateRenderState(inletIndex);
//    inletVals[inletIndex] = input;
//
//    System.out.println("INLET INPUT: " + input);
//    System.out.println("INLET INDEX: " + inletIndex);
//  }

  public void anything(java.lang.String message, Atom[] args) {
    int inletIndex = getInlet();
    updateRenderState(inletIndex);

    if (message == "float") {
      inletVals[inletIndex] = args[0].getFloat();
    } else if (message == "int") {
      inletVals[inletIndex] = args[0].getInt();
    } else { // if (message == "text") {
      inletVals[inletIndex] = args[0].toString(); // getString breaks for "123" or "123.45", doesn't break for "123.45.67"
    }
  }

  private void updateRenderState(int index) {
    if (
          index == Inlet.TOP_LEFT_X.ordinal() ||
          index == Inlet.TOP_LEFT_Y.ordinal() ||
              index == Inlet.BOTTOM_RIGHT_X.ordinal() ||
              index == Inlet.BOTTOM_RIGHT_Y.ordinal() ||
              index == Inlet.RED.ordinal() ||
              index == Inlet.GREEN.ordinal() ||
              index == Inlet.BLUE.ordinal() ||
              index == Inlet.BRIGHTNESS.ordinal()
        ) {
      lightsNeedRerender = true;
    }
  }

  public void bang() {
    if (((Integer) inletVals[Inlet.BANG_SHOULD_RESET.ordinal()]) == 0) {
      CompletableFuture.runAsync(() -> {
        try {
          runAnimation();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }, FX_RENDER_THREAD_POOL);
    } else {
      post("DEBUG: BANG B");
      String bridgeIp = (String) inletVals[Inlet.BRIDGE_IP.ordinal()];

      if (isValidInet4Address(bridgeIp)) {
        post("CONNECTING TO BRIDGE AT IP: " + bridgeIp);
        if (hue == null) {
          scheduleRenderCalls();
          hue = new HueConnectorEntertainment(bridgeIp);
        } else {

          try {
            RENDER_THREAD_POOL.shutdown();
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          hue.connectToBridge(bridgeIp);
          scheduleRenderCalls();
        }
      }
    }
  }

  private void runAnimation() {
    if (hue == null) return;
    boolean singleRandomLight = ((Integer) inletVals[Inlet.ANIMATION_AFFECT_SINGLE_RANDOM_LIGHT.ordinal()]) == 1;
    final AnimationType animationType = AnimationType.values()[(Integer) inletVals[Inlet.ANIMATION_TYPE.ordinal()]];
    if (singleRandomLight) {
      switch (animationType) {
        case STROBE:
          hue.strobeRandomLight(
              (Float) inletVals[Inlet.ANIMATION_TIME.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_RED.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_GREEN.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BLUE.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BRIGHTNESS.ordinal()]
          );
          break;
        case COOLDOWN_STROBE:
          hue.cooldownStrobeRandomLight(
              (Float) inletVals[Inlet.ANIMATION_TIME.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_RED.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_GREEN.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BLUE.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BRIGHTNESS.ordinal()]
          );
          break;
        default:
          break;
      }
    } else {
      switch (animationType) {
        case STROBE:
          hue.strobe(
              (Float) inletVals[Inlet.ANIMATION_TIME.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_TOP_LEFT_X.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_TOP_LEFT_Y.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BOTTOM_RIGHT_X.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BOTTOM_RIGHT_Y.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_RED.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_GREEN.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BLUE.ordinal()],
              (Float) inletVals[Inlet.ANIMATION_BRIGHTNESS.ordinal()]
          );
          break;
        case COOLDOWN_STROBE:
          hue.cooldownStrobe(
                  (Float) inletVals[Inlet.ANIMATION_TIME.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_TOP_LEFT_X.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_TOP_LEFT_Y.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_BOTTOM_RIGHT_X.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_BOTTOM_RIGHT_Y.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_RED.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_GREEN.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_BLUE.ordinal()],
                  (Float) inletVals[Inlet.ANIMATION_BRIGHTNESS.ordinal()]
          );
          break;
        default:
          break;
      }
    }
  }

//  private void printAllInlets() {
//    StringBuilder res = new StringBuilder();
//    for (int i = 0; i < inletVals.length; i++) {
//      res.append((Inlet.values())[i] + ": " + String.format("%.2f", (Float) inletVals[i]) + "\n");
//    }
//    System.out.println(res);
//  }

  private void scheduleRenderCalls() {
    RENDER_THREAD_POOL.scheduleAtFixedRate(() -> {
        boolean shouldLog = Math.random() < 1.0 / (38 * 5);
        if (shouldLog) {
          post("SCHEDULE RENDER CALL");
          post("Lights need rerender: " + lightsNeedRerender);
        }
        if (hue != null && (Integer) inletVals[Inlet.RENDER.ordinal()] == 1 && lightsNeedRerender) {
          if (shouldLog) {
            post("hue: " + hue);
          }
          lightsNeedRerender = false;
          try {
            hue.setColorForArea(
                    (Float) inletVals[Inlet.TOP_LEFT_X.ordinal()],
                    (Float) inletVals[Inlet.TOP_LEFT_Y.ordinal()],
                    (Float) inletVals[Inlet.BOTTOM_RIGHT_X.ordinal()],
                    (Float) inletVals[Inlet.BOTTOM_RIGHT_Y.ordinal()],
                    (Float) inletVals[Inlet.RED.ordinal()],
                    (Float) inletVals[Inlet.GREEN.ordinal()],
                    (Float) inletVals[Inlet.BLUE.ordinal()],
                    (Float) inletVals[Inlet.BRIGHTNESS.ordinal()]
            );
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
    }, RENDER_INIT_DELAY_MS, RENDER_PERIOD_MS, MILLISECONDS);
  }

  private static final String IPV4_REGEX =
          "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                  "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                  "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                  "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
  private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);
  private static boolean isValidInet4Address(String ip) {
    if (ip == null) {
      return false;
    }
    Matcher matcher = IPv4_PATTERN.matcher(ip);
    return matcher.matches();
  }

}
