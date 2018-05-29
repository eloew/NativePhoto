Android Implementaion

Open React Native android project in android studio

Copy PhotoModule.java and PhotoPackage.java to your project.

Open MainApplication.java

Add "new PhotoPackage()" line to the getPackages methoid:

   @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
              new PhotoPackage()
      );
    }

Open AndroidManifest.xml 
Add permisions and features.

<manifest>
    ...

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
    
    ...


iOS implementation
open ios .xcodeproj
Add PhotoUtil.m & PotoUtil.h to the xcode project
Righ click info.plist > Open As > Source Code
Add the below usage descriptions

<plist version="1.0">
<dict>
  <key>NSPhotoLibraryUsageDescription</key>
  <string>Access to photo library to add pictures</string>
  <key>NSCameraUsageDescription</key>
  <string>Access to camera to take pictures and videos</string>
  ...

