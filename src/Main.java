import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.DomainType;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.SupportedFeature;
import com.philips.lighting.hue.sdk.wrapper.domain.device.Device;
import com.philips.lighting.hue.sdk.wrapper.domain.device.DeviceState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightConfiguration;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightStateImpl;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupLightLocation;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupType;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

public class Main {

    private static class LightInfo {
        LightPositionLabel label; // Position on Yufi stage
        String lightId; // Assigned by bridge when discovered
        String serialNumber; // Written on bulb
        double xPos; // For hue entertainment
        double yPos; // For hue entertainment

        LightInfo(LightPositionLabel label, String lightId, String serialNumber, double xPos, double yPos) {
            this.label = label;
            this.lightId = lightId;
            this.serialNumber = serialNumber;
            this.xPos = xPos;
            this.yPos = yPos;
        }
    }

    private static final LightInfo[] LIGHTS = {
            new LightInfo(LightPositionLabel.F1, "21", "0ABC6F", -1,-1),
            new LightInfo(LightPositionLabel.F2, "20", "5F3C99", -0.66,-0.66),
            new LightInfo(LightPositionLabel.F3, "19", "AFB212", 0.66,-0.66),
            new LightInfo(LightPositionLabel.F4, "22", "63672A", 1,-1),

            new LightInfo(LightPositionLabel.M1, "18", "019B2C", -0.33,-0.33),
            new LightInfo(LightPositionLabel.M2, "16", "16A756", 0, 0),
            new LightInfo(LightPositionLabel.M3, "31", "E5E923", 0.33,-0.33),

            new LightInfo(LightPositionLabel.B1, "12", "5A7E3A", -0.25,1),
            new LightInfo(LightPositionLabel.B2, "14", "1E7EFB", 0,1),
            new LightInfo(LightPositionLabel.B3, "11", "9DC479", 0.25,1)
    };

    static LightInfo getLightInfo(LightPositionLabel label) {
        for (LightInfo light : LIGHTS) {
            if (light.label == label) {
                return light;
            }
        }
        return null;
    }

    // Streaming credentials (post request to <ip-address>/api). Ip for local is 169.254.8.173 (or 169.254.10.203 as of Mar 2022)
    // Note: Can find local IPs by running bridge discovery. Have main run `printAllBridgeIpsOnNetwork`
    // {
    //     "devicetype":"ableton#mymacbook"
    // }

        // [
    // 	{
    // 		"success": {
    // 			"username": "WZtGoHPHJvRmKFAJ6qOMfAg6huflerOxgVXG2rlB",
    // 			"clientkey": "011081C4176F68194BB5511B14D69D24"
    // 		}
    // 	}
    // ]

    // NEW (9/11/2018)
    // [
    //	{
    //		"success": {
    //			"username": "yBgR1WFTz-lEv1HS4PjmrOCF37ZiVxNPi0b2ZgUI"
    //		}
    //	}
    //]
    //
    // Update location info: https://developers.meethue.com/documentation/hue-entertainment-api
    // TLDR; go to http://10.0.1.2/debug/clip.html (or 169.254.8.173 on local)
    // do PUT on /api/yBgR1WFTz-lEv1HS4PjmrOCF37ZiVxNPi0b2ZgUI/groups/3
    // with body like:
    // {
    //	"locations": {
    //		"15": [
    //			0.4,
    //			0.5,
    //			0
    //		]
    //	}
    //}

    private static final int MAX_HUE = 65535;

    // IP when connected to router directly: 192.168.111.222
    private static final String IP_ADDRESS = "192.168.111.222"; // "192.168.0.22"
    private static final String GROUP_NAME = "Yufi";

    public static void main(String[] args) throws Exception {
        HueConnectorEntertainment h = new HueConnectorEntertainment(IP_ADDRESS, false);
        Thread.sleep(10000);
        setLightToRandomColor(h.bridge, "19");

//        printAllBridgeIpsOnNetwork();
//        System.out.println("Before:");
//        printAllDeviceIds(h.bridge);
//        setupLights(h.bridge);
//        System.out.println();
//        System.out.println("After:");
//        printAllDeviceIds(h.bridge);
        // new SetupLightsWithGui();
//        setupLights(h.bridge);
//        printEntertainmentGroupLightLocations(h.bridge);
//        setLightLocations(h.bridge);
//        setupSingleLight(h.bridge, LightPositionLabel.M3);
//        turnLightOnThenOff(h.bridge, "11", 2);
//        turnEachLightOnThenOffToFindId(h.bridge);
//        configureEntertainmentGroup(h.bridge);
//        allOnAllOff(h);


//         setupLights(h.bridge);
//        MaxPhilipsHueEntertainmentObject m = new MaxPhilipsHueEntertainmentObject();
        Thread.sleep(10000);
    }

    private static void printAllBridgeIpsOnNetwork() throws Exception {
        HueConnectorEntertainment h = new HueConnectorEntertainment(null, false);
        Thread.sleep(100000);
    }

    private static void turnEachLightOnThenOffToFindId(Bridge b) throws Exception {
        List<Device> lights = b.getBridgeState().getDevices(DomainType.LIGHT_POINT);
        for (Device light : lights) {
            System.out.println("TURNING ON AND OFF LIGHT WITH ID: " + light.getIdentifier());
            turnLightOnThenOff(b, light.getIdentifier(), 2);
            Thread.sleep(1000);
        }
    }

    private static void turnLightOnThenOff(Bridge b, String lightId, int numTimes) throws Exception {
        LightPoint light = (LightPoint) b.getBridgeState().getDevice(DomainType.LIGHT_POINT, lightId);

        LightConfiguration lightConfiguration = light.getLightConfiguration();
        HueColor color = new HueColor(
                new HueColor.RGB((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)),
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
        LightState lightState = new LightStateImpl();
        lightState.setBrightness(255);
        lightState.setXY(color.getXY().x, color.getXY().y);

        // Do it three times
        for (int i = 0; i < numTimes; i++) {
            lightState.setOn(true);
            updateLightState(light, lightState);
            Thread.sleep(1000);

            lightState.setOn(false);
            updateLightState(light, lightState);
            Thread.sleep(1000);
        }
    }

    private static void updateLightState(LightPoint light, LightState lightState) {
        light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
            @Override
            public void handleCallback(Bridge bridge, ReturnCode returnCode, List responses, List errors) {
                if (returnCode == ReturnCode.SUCCESS) {
                    // Cool
                } else {
                    System.out.println("ERROR UPDATING LIGHT STATE: ");
                    System.out.println(errors);
                }
            }
        });
    }

    private static void allOnAllOff(HueConnectorEntertainment h) throws Exception {
        for (int i = 0; i < 11; i++) {
            Thread.sleep(4000);
            // h.cooldownStrobe(1.0, -1, 1, 1, -1, 255, 255, 255, 255);
            if (i % 2 == 0) {
                h.setColorForArea(-1, 1, 1, -1, 255, 255, 255, 255);
            } else {
                h.setColorForArea(-1, 1, 1, -1, 255, 255, 255, 0);
            }
        }
    }



//    public static void main(String[] args) throws Exception {
//        HueConnectorEntertainment h = new HueConnectorEntertainment();
////        h.bridge.getBridgeConfiguration();
////        h.drawGroup2(0, 150, 0, true, 254, 254, 254);
////        Thread.sleep(1000);
////        h.randomizeLights();
//        Thread.sleep(10000);
//        // h.colorAll();
//
//        Thread.sleep(1000);
//        // h.setColorForArea(-1, 1, 1, -1, 10,0,0,30);
//        for (int i = 0; i < 20; i++) {
//            System.out.println("strobe");
//            // h.strobe(1, -1, 1, 1, -1, 255, 255, 255, 255);
//            h.setColorForArea(-1, 1, 1, -1, 255 * Math.random(), 255, 255, 100);
////            h.strobeRandomLight( 0.15, 255, 255, 255, 255);
//            Thread.sleep(3000);
//        }

//        for (int i = 0; i < 100; i++) {
////            h.draw(-1, 1, 1, -1, 1000, 130, Math.random(), Math.random(), Math.random());
////            h.colorAll2();
////            Thread.sleep(5000);
//            if (i % 2 != 0) {
////                h.turnAreaOff();
//                h.draw(-1, 1, 1, -1, 5000, 0.5, 0.125, 0.05);
//            } else {
//                //h.draw(-1, 1, 1, -1, 5000, 0.5, 0.125, 0.05, 1.0, 0.25, 0.1);
//                h.colorAll();
//            }
//            Thread.sleep(10000);
//        }
//
//        Thread.sleep(10000);
//    }

//    private static final String[] SERIALS = {
//        "0ABC6F", // F1
//        "5A7E3A",  // B1
//        "1E7EFB",  // B2
//        "9DC479",  // B3
//        "16A756",  // M2
//        // "46182C", // old M3
//        "E5E923", // M3
//        // "827F70", // old F4
//        "63672A", // F4
//        "019B2C", // M1
//        "5F3C99", // F2
//        "AFB212" // F3
//    };

    private static List<String> getBridgeLightIds(Bridge bridge) {
        return bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT).stream().map(
                device -> device.getIdentifier()
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    private static void configureEntertainmentGroup(Bridge bridge) {
        final Group group = getEntertainmentGroup(bridge);
        if (group != null) {
            System.out.println("FOUND ENTERTAINMENT GROUP WITH NAME: " + GROUP_NAME);
        } else {
            System.out.println("COULD NOT FIND ENTERTAINMENT GROUP WITH NAME: " + GROUP_NAME);
        }

        System.out.println("vvvvv OLD GROUP INFO vvvvv");
        printGroupInfo(group);

        // Update to reflect LIGHTS array
//        group.setLightIds(Arrays.stream(LIGHTS).map(li -> li.lightId).collect(Collectors.toList()));
        group.setLightLocations(Arrays.stream(LIGHTS).map(Main::lightInfo2GroupLightLocation).collect(Collectors.toList()));
        bridge.updateResource(group, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
            @Override
            public void handleCallback(Bridge bridge, ReturnCode returnCode, List responses, List errors) {
                if (returnCode == ReturnCode.SUCCESS) {
                    System.out.println("vvvvv NEW GROUP INFO vvvvv");
                    printGroupInfo(group);
                } else {
                    System.out.println("ERROR UPDATING BRIDGE GROUP");
                }
            }
        });
    }

    private static GroupLightLocation lightInfo2GroupLightLocation(LightInfo li) {
        GroupLightLocation loc = new GroupLightLocation();
        loc.setLightIdentifier(li.lightId);
        loc.setX(li.xPos);
        loc.setY(li.yPos);
        loc.setZ(new Double(0));
        return loc;
    }

    private static void printGroupInfo(Group g) {
        System.out.println("LIGHT IDS:\t" + g.getLightIds());
        String locString = "";
        for (GroupLightLocation loc : g.getLightLocations()) {
            locString += ", " + loc.getLightIdentifier() +  " -> (" + loc.getX() + ", " + loc.getY() + ")";
        }
        System.out.println("LIGHT LOCS:\t" + locString.substring(2));
    }

    private static void setupSingleLight(Bridge bridge, LightPositionLabel pos) throws Exception {
        List<String> prevDeviceIds = getBridgeLightIds(bridge);
        bridge.findNewDevices(Collections.singletonList(getLightInfo(pos).lightId));
        Thread.sleep(30000);

        List<String> newDeviceIds = getBridgeLightIds(bridge);

        System.out.println("Prev device IDs size: " + prevDeviceIds.size());
        System.out.println("Cur device IDs size: " + newDeviceIds.size());
        System.out.println();
        System.out.println("Prev device IDs: " + prevDeviceIds);
        System.out.println("Cur device IDs: " + newDeviceIds);

    }

    private static void setupLights(Bridge bridge) throws Exception {
        List<String> serials = Arrays.stream(LIGHTS).map((li) -> li.serialNumber).collect(Collectors.toList());
        bridge.findNewDevices(serials);
        Thread.sleep(60000);
        List<String> lightIds = Arrays.asList(
            (String[]) bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT).stream().map(
                device -> device.getIdentifier()
            ).toArray()
        );

        Group g = getEntertainmentGroup(bridge);
        g.setLightIds(lightIds);
    }

    private static void printAllDeviceIds(Bridge bridge) {
        for (Device d : bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT)) {
            System.out.println(((LightPoint) d).getIdentifier());
        }
    }

//    private static void createStageLights(Bridge bridge) {
//        List<LightPoint> validLights = getValidLights(bridge);
//
//
//    }
//
//    private static List<LightPoint> getValidLights(Bridge bridge) {
//        ArrayList<LightPoint> validLights = new ArrayList<LightPoint>();
//        for (final LightPoint light : bridge.getBridgeState().getLights()) {
//            if (light.getInfo().getSupportedFeatures().contains(SupportedFeature.STREAM_PROXYING)) {
//                validLights.add(light);
//            }
//        }
//        return validLights;
//    }

//    private static class SetupLightsWithGui {
//        /**
//         * Create the GUI and show it.  For thread safety,
//         * this method should be invoked from the
//         * event-dispatching thread.
//         */
//        private void createAndShowGUI() {
//            //Create and set up the window.
//            JFrame frame = new JFrame("HelloWorldSwing");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//            //Add the ubiquitous "Hello World" label.
//            JLabel label = new JLabel("Hello World");
//            frame.getContentPane().add(label);
//
//            //Display the window.
//            frame.pack();
//            frame.setVisible(true);
//        }
//
//        public SetupLightsWithGui() {
//            //Schedule a job for the event-dispatching thread:
//            //creating and showing this application's GUI.
//            javax.swing.SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    createAndShowGUI();
//                }
//            });
//        }
//    }

    public static void setLightLocations(Bridge bridge) {
        Scanner input = new Scanner(System.in);

        List<Device> devices = bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT);
        devices.sort((a, b) -> a.getIdentifier().compareTo(b.getIdentifier()));
        List<String> ids = new ArrayList<String>();
        for (Device d : devices) {
            ids.add(d.getIdentifier());
        }

        String response = "";
        while (!response.equals("quit")) {
            System.out.print("'pl' to print locations, input device ID to flash, append x and y positions to set location (available IDs: " +
            Arrays.toString(ids.toArray()) + "): ");
            response = input.nextLine();
            if (!response.equals("quit")) {
                String[] arr = response.split(" ");
                if (arr.length == 1) {
                    if (arr[0].equals("pl")) {
                        printEntertainmentGroupLightLocations(bridge);
                    } else {
                        setLightToRandomColor(bridge, arr[0]);
                    }
                } else {
                    setLightLocation(bridge, arr[0], Double.parseDouble(arr[1]), Double.parseDouble(arr[2]));
                }
            }
        }

    }

    private static void setLightToRandomColor(Bridge b, String id) {
        LightPoint light = (LightPoint)b.getBridgeState().getDevice(DomainType.LIGHT_POINT, id);
        LightConfiguration lightConfiguration = light.getLightConfiguration();
        HueColor color = new HueColor(
            new HueColor.RGB(((int) (Math.random() * 255)), ((int) (Math.random() * 255)), ((int) (Math.random() * 255))),
            lightConfiguration.getModelIdentifier(),
            lightConfiguration.getSwVersion());

        LightState lightState = new LightStateImpl();
        lightState.setBrightness(255);
        lightState.setXY(color.getXY().x, color.getXY().y);
        light.updateState(lightState);
        //LightState newState = b.getBridgeState().getLight(id).getLightState().setHue((int) (Math.random() * MAX_HUE));
        b.getBridgeState().getLightPoint(id).updateState(lightState);

    }

    private static void setLightLocation(Bridge bridge, String id, double x, double y) {
        GroupLightLocation location = new GroupLightLocation();
        location.setLightIdentifier(id);
        location.setX(x);
        location.setY(y);

        Group entertainmentGroup = getEntertainmentGroup(bridge);
//        entertainmentGroup.removeLightLocation(id);
        entertainmentGroup.addLightLocation(location);
//        entertainmentGroup.removeLight(bridge.getBridgeState().getLightPoint(id));
//        bridge.getBridgeState().getGroup(entertainmentGroup.getIdentifier())
    }

    private static void printEntertainmentGroupLightLocations(Bridge bridge) {
        Group g = getEntertainmentGroup(bridge);
        for (GroupLightLocation loc : g.getLightLocations()) {
            System.out.println(loc.getLightIdentifier() + ": " + loc.getX() + ", " + loc.getY());
        }
    }

    private static Group getEntertainmentGroup(Bridge bridge) {
        Group entertainmentGroup = null;
        for (Group group : bridge.getBridgeState().getGroups()) {
            if (group.getGroupType() == GroupType.ENTERTAINMENT && group.getName().equals(GROUP_NAME)) {
                return group;
            }
        }
        return null;
    }

    private static class XY {
        double x;
        double y;
        XY(double x, double y) {
            x = x;
            y = y;
        }
    }

    private static enum LightPositionLabel {
        F1,
        F2,
        F3,
        F4,

        M1,
        M2,
        M3,

        B1,
        B2,
        B3
    }

}
