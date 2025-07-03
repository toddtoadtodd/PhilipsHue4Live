# PhilipsHue4Live
Philips Hue integration with Ableton Live (Using Max4Live)

# Setup

Add the following to your:

(Mac) /Applications/Ableton Live 10 Suite.app/Contents/App-Resources/Max/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/max.java.config.txt
(Windows) C:\ProgramData\Ableton\Live 11 Suite\Resources\Max\resources\packages\max-mxj\java-classes\max.java.config.txt

(Mac)
max.system.jar.dir /Users/todd/Documents/code/PhilipsHue4Live/HueSDK/Apple/MacOS/Java/
max.system.class.dir /Users/todd/Documents/code/PhilipsHue4Live/out/production/PhilipsHue4Live

(Windows)
max.system.jar.dir C:\\Users\\toddm\\code\\PhilipsHue4Live\\HueSDK\\Windows
max.system.class.dir C:\\Users\\toddm\\code\\PhilipsHue4Live\\out\\production\\PhilipsHue4Live





Intellij setup (Mac):

1. Open this directory (PhilipsHue4Live) in Intellij
2. Open file "src/Main.java"
3. Setup JDK from there (should have option at top of file)
4. (probably skip this step) At bottom of file, it should say "Maven build script found". Confirm to load dependencies.
5. Go to File -> Project Structure, select "Modules", change "src" directory in PhilipsHue4Live to mark as "Sources" (makes it blue, runnable).
6. On right in "Dependencies", add huecpp-wrapper.jar (in MacOS HueSDK folder), libhuestream_java_managed.jar, max.jar.
7. Update HueConnectorEntertainment file paths in code.

Now, it won't run if it's an Apple Silicon Mac. Attempting to run will get the error:
"... (mach-o file, but is an incompatible architecture (have 'x86_64', need 'arm64e' or 'arm64'))"


