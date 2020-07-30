 
# OpenLibreNFC

## Disclaimer
Do not use sensors modified with this app to make medical decisions. I am not responsible to any damage to the sensor that might occur, use this at your own risk. This app is only intended for research.

## Installation
To use this app, you'll have to build it yourself.

You'll need the Android SDK installed and define the ANDROID_SDK_ROOT environment variable, after that, you can run gradlew to build the .apk file (it should probably be signed so you can install it).

You can also install Android Studio and build it from there.

## Usage
When you open the app, you can select different options (Scan, Reset Age, Start...) and then scan the sensor, which will execute whatever you selected.
When you scan the sensor to perform any action, you should wait for the second vibration to stop scanning. This might take a bit longer with operations which involve writing into memory.

The "Dump Memory" option will dump 360 bytes of the sensor's FRAM to `sdcard/openlibrenfc/memory_dump.txt`, and the "Load Memory" will load that same file. If you edit that file, the checksums won't be checked, and it might send the sensor into failure if a checksum is incorrect. 
Make sure you have **backups** of previous memory dumps so you can restore it in case anything happens.

