import java.io.IOException;

import com.lighting.huestream.Config;
import com.lighting.huestream.ConstantAnimation;
import com.lighting.huestream.CurveAnimation;
import com.lighting.huestream.ExplosionEffect;
import com.lighting.huestream.PersistenceEncryptionKey;
import com.lighting.huestream.Point;
import com.lighting.huestream.PointVector;
import com.lighting.huestream.RandomAnimation;
import com.lighting.huestream.TweenType;
import com.lighting.huestream.HueStream;
import com.lighting.huestream.Area;
import com.lighting.huestream.AreaEffect;
import com.lighting.huestream.Bridge;
import com.lighting.huestream.BridgeStatus;
import com.lighting.huestream.Color;

import com.lighting.huestream.FeedbackMessage;
import com.lighting.huestream.FeedbackMessage.FeedbackType;
import com.lighting.huestream.IFeedbackMessageHandler;
import com.lighting.huestream.LightSourceEffect;
import com.lighting.huestream.Location;

public class HueEdkExample {
    static {
        System.loadLibrary("lib/huestream_java_native");
    }

    private static HueStream hueStream;
    private static FeedBackHandler feedbackHandler;

    public static void main(String[] args) {
        Init();
        hueStream.ConnectBridge();

        IfMultipleGroupsSelectFirst();

        SetLightsToGreen(); //Replace by ShowAnimatedLightsExample(); for more advanced effects
        // ShowAnimatedLightsExample();

        WaitForEnterToQuit();
        hueStream.ShutDown();
    }

    public static void Init() {
        System.out.println("Welcome to this HueStream minimal example Java app.\n"
                + "The only thing it does is turn all lights in the entertainment area green.");

        Config config = new Config("JavaHueExample", "PC", new PersistenceEncryptionKey("jfsDn39fqSyd0fvfn"));
        hueStream = new HueStream(config);

        feedbackHandler = new FeedBackHandler();
        hueStream.RegisterFeedbackHandler(feedbackHandler);
    }

    public static void IfMultipleGroupsSelectFirst() {
        Bridge bridge = hueStream.GetLoadedBridge();
        hueStream.SelectGroup(bridge.GetGroups().get(0));
//        System.out.println("NAMEEE: " + bridge.getGroups().get(1).getName());
        if (bridge.GetStatus() == BridgeStatus.BRIDGE_INVALID_GROUP_SELECTED) {
            hueStream.SelectGroup(bridge.GetGroups().get(0));
        }
    }

    public static void SetLightsToGreen() {
        AreaEffect effect = new AreaEffect("", 0);
        effect.AddArea(Area.getAll());
        Color c = new Color(0,1,0);
        c.ApplyBrightness(254);
        effect.SetFixedColor(c);
        effect.Enable();
        hueStream.LockMixer();
        hueStream.AddEffect(effect);
        hueStream.UnlockMixer();
    }

    public static void ShowAnimatedLightsExample() {
        //Create an animation which is fixed 0
        ConstantAnimation fixedZero = new ConstantAnimation(0.0);

        //Create an animation which is fixed 1
        ConstantAnimation fixedOne = new ConstantAnimation(1.0);

        //Create an animation which repeats a 2 second sawTooth 5 times
        PointVector pointList = new PointVector();
        pointList.add(new Point(   0, 0.0));
        pointList.add(new Point(1000, 1.0));
        pointList.add(new Point(2000, 0.0));
        double repeatTimes = 5;
        CurveAnimation sawTooth = new CurveAnimation(repeatTimes, pointList);

        //Create an effect on the left half of the room where blue is animated by sawTooth
        AreaEffect leftEffect = new AreaEffect("LeftArea", 1);
        leftEffect.AddArea(Area.getLeftHalf());
        leftEffect.SetColorAnimation(fixedZero, fixedZero, sawTooth);

        //Create a red virtual light source where x-axis position is animated by sawTooth
        LightSourceEffect rightEffect = new LightSourceEffect("RightSource", 1);
        rightEffect.SetFixedColor(new Color(0,1,0));
        rightEffect.SetPositionAnimation(sawTooth, fixedZero);
        rightEffect.SetRadiusAnimation(fixedOne);

        //Create effect from predefined explosionEffect
        ExplosionEffect explosion = new ExplosionEffect("explosion", 2);
        Color explosionColorRGB = new Color(1.0, 0.8, 0.4);
        Location explosionLocationXY = new Location(0, 1);
        double radius = 2.0;
        double duration_ms = 2000;
        double expAlpha_ms = 50;
        double expRadius_ms = 100;
        explosion.PrepareEffect(explosionColorRGB, explosionLocationXY, duration_ms, radius, expAlpha_ms, expRadius_ms);

        //Now play all effects
        hueStream.LockMixer();
        hueStream.AddEffect(leftEffect);
        hueStream.AddEffect(rightEffect);
        hueStream.AddEffect(explosion);
        leftEffect.Enable();
        rightEffect.Enable();
        explosion.Enable();
        hueStream.UnlockMixer();
    }

    public static void playCandle() {
        AreaEffect candle = new AreaEffect("candle", 1);
        candle.AddArea(Area.getAll());
        candle.SetFixedColor(new Color(1.0, 0.8, 0.4));
        RandomAnimation i = new RandomAnimation(0.2, 0.6, 200, 400, TweenType.EaseInOutQuad);
        candle.SetIntensityAnimation(i);

        hueStream.LockMixer();
        hueStream.AddEffect(candle);
        candle.Enable();
        hueStream.UnlockMixer();
    }

    public static void WaitForEnterToQuit() {
        System.out.println("Press enter to quit");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class FeedBackHandler extends IFeedbackMessageHandler {
        public void NewFeedbackMessage(FeedbackMessage message)
        {
            System.out.println(message.GetDebugMessage());
            if (message.GetMessageType() == FeedbackType.FEEDBACK_TYPE_USER) {
                System.out.println(message.GetUserMessage());
            }
        }
    }
}