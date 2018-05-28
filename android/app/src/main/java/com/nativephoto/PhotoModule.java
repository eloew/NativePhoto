package com.nativephoto;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.facebook.common.internal.Closeables;
import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static android.app.Activity.RESULT_OK;

public class PhotoModule extends ReactContextBaseJavaModule implements PermissionListener, ActivityEventListener{

    private static final String TAG = "ReactNativeJS";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_EXTERNAL_STORAGE_CAMERA = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PHOTO = 997;
    private static final int REQUEST_CAMERA = 998;
    public static final int REQUEST_VIDEO = 999;

    private ReactApplicationContext reactContext;
    private Callback tokenCallback;
    private Promise promise;
    private ReadableMap options;

    public PhotoModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        reactContext.addActivityEventListener(this);

    }

    //<editor-fold desc="Gallery">

    @ReactMethod
    public void selectPhoto(final ReadableMap options, final Promise promise) {
        this.options = options;
        this.promise = promise;

        if (CheckStoragePermision() == true)
            ShowPhotoGallery();
        else
            requestStoragePermissions(REQUEST_EXTERNAL_STORAGE);
    }


    public static final int RC_PhotoGallery = 998;

    private void ShowPhotoGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //
        boolean allowMultiple = false;
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        reactContext.getCurrentActivity().startActivityForResult(intent, RC_PhotoGallery);
    }

    private void processPhotoGalery(Intent data)
    {
        Uri selectedUri = data.getData();
        Uri uri = null;
        if (options.hasKey("height") && options.hasKey("width")) {
            String pathName = getPathName(data.getData());

            int height = options.getInt("height");
            int width = options.getInt("width");
            Bitmap bitmap = decodeSampledBitmapFromResource(pathName ,height, width);
            uri = getImageUri(reactContext, bitmap);
        }
        else {
            uri = selectedUri;
        }

        String path = getPathName(selectedUri);

        promiseFilePath(uri, selectedUri, path);


    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void processPhotoGalery1(Intent data)
    {

        try {
            Uri uri = data.getData();



            String fileName = getFileName(uri);

            //Log.d(TAG, "processPhotoGalery: uri: " + uri.toString());
            File file = uriTofile(uri, fileName);
            //uri = Uri.fromFile(file);


            uri = resizeImage(file);
            promiseFilePath(uri, null, uri.toString());


        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            Log.d(TAG, "processPhotoGalery Error: " + message);
            promiseError(message);
        }

    }

    private File uriTofile(Uri uri, String fileName) throws Exception {
        File file = new File(reactContext.getExternalCacheDir(), fileName);
        InputStream inputStream = reactContext.getContentResolver().openInputStream(uri);
        OutputStream outputStream = new FileOutputStream(file);

        try {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } finally {
            outputStream.close();
            outputStream.close();
        }

        return file;
    }



    //</editor-fold>

    //<editor-fold desc="Camera">

    @ReactMethod
    public void showCamera(final ReadableMap options, final Promise promise) {
        this.options = options;
        this.promise = promise;

        if (CheckCameraPermission() == true && CheckStoragePermision() == true)
            dispatchTakePictureIntent();
        else
            requestCameraPermision(REQUEST_CAMERA);
    }

    private String mCurrentPhotoPath;
    Uri photoURI;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(reactContext.getCurrentActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //photoURI = FileProvider.getUriForFile(reactContext, "com.nativephoto.fileprovider", photoFile);
                photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                reactContext.getCurrentActivity().startActivityForResult(takePictureIntent, REQUEST_PHOTO);
            }

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = reactContext.getCurrentActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //String dir = getFilesDir().getAbsolutePath();
        //File storageDir = getActivity().getFilesDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }



    private void processCamera() {
        //File file = new File(mCurrentPhotoPath);
        //Uri uri = Uri.fromFile(file);

        Uri selectedUri = photoURI;
        Uri uri = null;
        if (options.hasKey("height") && options.hasKey("width")) {
            int width = options.getInt("width");
            int height = options.getInt("height");
            Bitmap bitmap = decodeSampledBitmapFromResource(mCurrentPhotoPath, height, width);
            uri = getImageUri(reactContext, bitmap);
        }
        else
        {
            uri = photoURI;
        }
        promiseFilePath(uri, selectedUri, selectedUri.toString());
    }


    //</editor-fold>

    //<editor-fold desc="Video">
    private String currentVideoPath;

    @ReactMethod
    public void showVideo(final Promise promise)
    {
        this.promise = promise;

        if (CheckCameraPermission() == true && CheckStoragePermision() == true)
            showVideoIntent();
        else
            requestCameraPermision(REQUEST_VIDEO);

    }

    private void showVideoIntent() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(reactContext.getCurrentActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            if (videoFile != null) {
                //Uri uri = FileProvider.getUriForFile(reactContext, "com.nativephoto.fileprovider", videoFile);
                Uri uri = Uri.fromFile(videoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                reactContext.getCurrentActivity().startActivityForResult(intent, REQUEST_VIDEO);
            }


        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "MPG4_" + timeStamp + "_";
        File storageDir = reactContext.getCurrentActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File videoFile = File.createTempFile(imageFileName,".mpg4", storageDir );

        currentVideoPath = videoFile.getAbsolutePath();
        return videoFile;
    }


    //</editor-fold>

    //<editor-fold desc="base64">

    @ReactMethod
    public void toBase64(String input, final Promise promise) {
        this.promise = promise;

        input = cleanPath(input);
        Log.d(TAG, "readImage: " + input);


        File file = new File(input);
        if (file.exists()) {

            Uri uri = Uri.fromFile(file);

            String base64 = toBase64(uri);
            if (base64 != null)
                promiseBase64(base64);
        }
        else {
            promiseError("No such file: " + input);
        }
    }

    private String toBase64(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = getBitmapFromUri(getReactApplicationContext(), uri);
            Log.d(TAG, "readImage bitmap null: " + (bitmap == null));
        } catch (FileNotFoundException e) {
            Log.d(TAG, "readImage error: " + e.getMessage());
            promiseError("toBase64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        String base64 = null;
        if (bitmap != null) {

            base64 = bitmapToBase64(bitmap);
        }
        else {
            promiseError("Can't read image;");
        }
        return base64;
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws FileNotFoundException {
        Log.d(TAG, "getBitmapFromUri: ");
        final InputStream imageStream = context.getContentResolver().openInputStream(uri);
        try {
            return BitmapFactory.decodeStream(imageStream);
        }
        catch (Exception e) {
            Log.d(TAG, "getBitmapFromUri error: " + e.getMessage());
            return null;
        }
        finally {
            Closeables.closeQuietly(imageStream);
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        Log.d(TAG, "bitmapToBase64: ");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private String cleanPath(String path) {
        //file:///storage/emulated/0/Android/data/com.riops2_camera/files/Pictures/image-de8dfe39-03b9-4e76-abc3-27535cfef83a.jpg
        int pos = path.indexOf("file:///");
        if (pos > -1 ) {
            return path.substring(8);
        }
        else {
            return path;
        }
    }

    private void promiseBase64(String base64) {
        WritableMap map = Arguments.createMap();
        map.putString("base64", base64);
        promise.resolve(map);

    }


    //</editor-fold>

    //<editor-fold desc="Helper Methods">

    private void promiseFilePath(@Nullable Uri uri, @Nullable Uri selectedUri, @Nullable String path) {
        WritableMap map = Arguments.createMap();
        Log.d(TAG, "promiseFilePath: uri: " + uri.toString());
        map.putString("uri", uri.toString());

        Log.d(TAG, "promiseFilePath: selectedUri: " + selectedUri.toString());
        map.putString("selectedUri", selectedUri.toString());

        Log.d(TAG, "promiseFilePath: path: " + path);
        map.putString("path", path);
        promise.resolve(map);

    }

    private void promiseError(String message) {
        promise.reject("", message, new Exception(message));
    }

    private void createThumbnail() {
        //Bitmap thumb = ThumbnailUtils.createVideoThumbnail(mediaItem.CurrentFile.GetFilePath(), MediaStore.Images.Thumbnails.MINI_KIND);
    }


    //https://developer.android.com/topic/performance/graphics/load-bitmap?hl=es
    public static Bitmap decodeSampledBitmapFromResource(String pathName
            , int reqHeight, int reqWidth) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Uri resizeImage(File imgFileOrig) throws Exception {

        Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());
// original measurements
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();

        final int destWidth = options.getInt("width");
        if(origWidth > destWidth){
            int destHeight = origHeight/( origWidth / destWidth ) ;// picture is wider than we want it, we calculate its target height

            // we create an scaled bitmap so it reduces the image, not just trim it
            Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b2.compress(Bitmap.CompressFormat.JPEG,70 , outStream);
            // we save the file, at least until we have made use of it
            File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "test.jpg");
            f.createNewFile();
            //write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(outStream.toByteArray());
            // remember close de FileOutput
            fo.close();
            return Uri.fromFile(f);
        }
        return null;
    }

    private String getPathName(Uri uri)
    {

        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor =
                getCurrentActivity().getContentResolver().query(uri, filePathColumn, null, null, null);
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String pathName = cursor.getString(columnIndex);

            return pathName;

        }
        cursor.close();
        return "";
    }

    private String getFileName(Uri uri)
    {

        String[] filePathColumn = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE};
        Cursor cursor =
                getCurrentActivity().getContentResolver().query(uri, filePathColumn, null, null, null);
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            int fileNameIndex = cursor.getColumnIndex(filePathColumn[1]);
            String fileName = cursor.getString(fileNameIndex);
            int mimeTypeIndex = cursor.getColumnIndex(filePathColumn[2]);
            String mimeType = cursor.getString(mimeTypeIndex);

            // Here we get the extension you want
            String extension = fileName.replaceAll("^.*\\.", "");
            Log.d(TAG, "getFileName: " + extension);
            return fileName;

        }
        cursor.close();
        return "";
    }

//</editor-fold>

    //<editor-fold desc="Permisions">

    protected boolean CheckStoragePermision()
    {
        if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    protected void requestStoragePermissions(int requestCode) {
        int permission = ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(reactContext.getCurrentActivity(),
                    PERMISSIONS_STORAGE,
                    requestCode
            );
        }
    }

    protected boolean CheckCameraPermission()
    {
        if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    private void requestCameraPermision(int requestCode) {
        //ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        //reactContext.getCurrentActivity().requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        Activity activity = getCurrentActivity();
        ((ReactActivity) activity).requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},  requestCode, this);

    }

    private void requestCameraPermision1() {
        ActivityCompat.requestPermissions(getCurrentActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }
    //</editor-fold>

    //<editor-fold desc="PermissionListener">

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    if (CheckCameraPermission() && CheckStoragePermision())
                        dispatchTakePictureIntent();
                    else
                        promiseError("Permission denied.");
                case REQUEST_VIDEO:
                    if (CheckCameraPermission() && CheckStoragePermision())
                        dispatchTakePictureIntent();

                    break;
                case REQUEST_EXTERNAL_STORAGE:
                    if (CheckCameraPermission())
                        ShowPhotoGallery();
                    else
                        promiseError("Permission denied.");
            }
        }
        else {
            promiseError("Permission denied.");//Permission denied, boo! Disable the functionality that depends on this permission.
        }
        return true;
    }

    //</editor-fold>

    //<editor-fold desc="ActivityEventListener">

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PHOTO:
                    processCamera();
                    break;
                case RC_PhotoGallery:
                    processPhotoGalery(data);
                    break;
                case REQUEST_VIDEO:
                    Uri videoUri = data.getData();  //videoUri.getPath()
                    promiseFilePath(videoUri, null, null);
                    //promiseFilePath(currentVideoPath);
                    break;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        //not used
    }


    //</editor-fold>

    //<editor-fold desc="ReactContextBaseJavaModule">

    @Override
    public String getName() {
        return "PhotoUtil";
    }




    //</editor-fold>

}
