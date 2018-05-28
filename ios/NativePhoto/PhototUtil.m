//
//  PhotoUtil.m
//  NativePhoto
//
//  Created by Eric Loew on 5/20/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "PhotoUtil.h"

#import <AVFoundation/AVFoundation.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <Photos/Photos.h>
#import <React/RCTUtils.h>

@import MobileCoreServices;

@interface PhotoUtil ()

//@property (nonatomic, strong) UIAlertController *alertController;
@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, strong) NSDictionary * options;
@property (nonatomic, strong) RCTPromiseRejectBlock reject;
@property (nonatomic, strong) RCTPromiseResolveBlock resolve;

//@property (nonatomic, strong) RCTResponseSenderBlock callback;
//@property (nonatomic, strong) NSDictionary *defaultOptions;
//@property (nonatomic, retain) NSMutableDictionary *options, *response;
//@property (nonatomic, strong) NSArray *customButtons;

@end

@implementation PhotoUtil

RCT_EXPORT_MODULE();
//RCT_EXPORT_METHOD(launchCamera:(NSDictionary *)options callback:(RCTResponseSenderBlock) callback)


RCT_EXPORT_METHOD(selectPhoto:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  self.options = options;
  self.resolve = resolve;
  self.reject = reject;

  self.picker = [[UIImagePickerController alloc] init];
  
  self.picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
  
  self.picker.mediaTypes = @[(NSString *)kUTTypeImage];
  self.picker.allowsEditing = YES;
  self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
  self.picker.delegate = self;
  
  // Check permissions
  void (^showPickerViewController)() = ^void() {
    
    dispatch_async(dispatch_get_main_queue(), ^{
      UIViewController *root = RCTPresentedViewController();
      [root presentViewController:self.picker animated:YES completion:nil];

    });
  };
  
  [self checkPhotosPermissions:^(BOOL granted) {
    if (!granted) {
       [self promiseError:self.reject message:@"Photo library permissions not granted"];
       return;
    }
    
    showPickerViewController();
  }];
}

RCT_EXPORT_METHOD(showCamera:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  self.options = options;
  self.resolve = resolve;
  self.reject = reject;
  
#if TARGET_IPHONE_SIMULATOR
  [self promiseError:reject message:@"Camera not available on simulator"];
  return;
#endif
  
   self.picker = [[UIImagePickerController alloc] init];
   self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
  self.picker.cameraDevice = UIImagePickerControllerCameraDeviceRear;
  
}

RCT_EXPORT_METHOD(showVideo:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  self.resolve = resolve;
  self.reject = reject;

#if TARGET_IPHONE_SIMULATOR
  [self promiseError:self.reject message:@"Camera not available on simulator"];
  return;
#endif
  
   self.picker = [[UIImagePickerController alloc] init];
   self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
  self.picker.cameraDevice = UIImagePickerControllerCameraDeviceRear;
  self.picker.videoQuality = UIImagePickerControllerQualityTypeHigh;
  
}


//
- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
  dispatch_block_t dismissCompletionBlock = ^{
    NSURL *imageURL = [info valueForKey:UIImagePickerControllerReferenceURL];
    NSString *mediaType = [info objectForKey:UIImagePickerControllerMediaType];
    
    NSString *fileName;
    if ([mediaType isEqualToString:(NSString *)kUTTypeImage]) {
      NSString *tempFileName = [[NSUUID UUID] UUIDString];
      if (imageURL && [[imageURL absoluteString] rangeOfString:@"ext=GIF"].location != NSNotFound) {
        fileName = [tempFileName stringByAppendingString:@".gif"];
      }
      else {
        fileName = [tempFileName stringByAppendingString:@".jpg"];
      }
    }
    else {
      NSURL *videoURL = info[UIImagePickerControllerMediaURL];
      fileName = videoURL.lastPathComponent;
    }
    
    // We default to path to the temporary directory
    NSString *path = [[NSTemporaryDirectory()stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    
    UIImage *image = [info objectForKey:UIImagePickerControllerEditedImage];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    path = [documentsDirectory stringByAppendingPathComponent:fileName];
    
    NSData *data = UIImagePNGRepresentation(image);
    [data writeToFile:path atomically:YES];
    
    [self promiseFilePath:self.resolve absolutePath: path];
    
    

    
  };
 
  dispatch_async(dispatch_get_main_queue(), ^{
    [picker dismissViewControllerAnimated:YES completion:dismissCompletionBlock];
  });
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
  [self promiseError:self.reject message:@"User Canceled"];
}

#pragma mark - base64

- (NSString*)toBase64:(NSString *)input
{
  NSRange range = [input rangeOfString:@"/" options:NSBackwardsSearch];
  NSLog(@"a :: %d   ",range.location);
  NSString *fileName = [input substringFromIndex:range.location + 1];
  NSLog(@"%@",fileName);
  UIImage *img = [self loadImage:fileName];
  NSLog(@"img = %@", img);
  
  NSData *imageData = UIImageJPEGRepresentation([UIImage imageWithCGImage:img.CGImage], 0.1);
  NSLog(@"5  %@ " , imageData);
  // Convert to base64 encoded string
  NSString *base64Encoded = [imageData base64EncodedStringWithOptions:0];
  NSLog(@"6  %@" , base64Encoded);
  return base64Encoded;
  
  /*
   NSLog(@"input = %@", input);
   NSData *imgData = [[NSData alloc] initWithContentsOfURL:[NSURL fileURLWithPath:input]];
   NSLog(@"imgData = %@", imgData);
   UIImage *thumbNail = [[UIImage alloc] initWithData:imgData];
   NSLog(@"thumbNail = %@", thumbNail);
   */
}

- (UIImage*)loadImage:(NSString *)input
{
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                       NSUserDomainMask, YES);
  NSString *documentsDirectory = [paths objectAtIndex:0];
  NSString* path = [documentsDirectory stringByAppendingPathComponent:
                    [NSString stringWithString: input] ];
  UIImage* image = [UIImage imageWithContentsOfFile:path];
  return image;
}
#pragma mark - Helpers

- (void)checkCameraPermissions:(void(^)(BOOL granted))callback
{
  AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
  if (status == AVAuthorizationStatusAuthorized) {
    callback(YES);
    return;
  } else if (status == AVAuthorizationStatusNotDetermined){
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
      callback(granted);
      return;
    }];
  } else {
    callback(NO);
  }
}

- (void)checkPhotosPermissions:(void(^)(BOOL granted))callback
{
  PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
  if (status == PHAuthorizationStatusAuthorized) {
    callback(YES);
    return;
  } else if (status == PHAuthorizationStatusNotDetermined) {
    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
      if (status == PHAuthorizationStatusAuthorized) {
        callback(YES);
        return;
      }
      else {
        callback(NO);
        return;
      }
    }];
  }
  else {
    callback(NO);
  }
}

//[array insertObject:obj atIndex:index];
- (void)promiseFilePath:(RCTPromiseResolveBlock)resolve absolutePath:(NSString *)absolutePath  {
  
  //NSString* base64 = [self toBase64: absolutePath];
  
  resolve(@{
            @"uri": absolutePath,
            @"base64": @""
            });
}

// [self promiseError:reject message:@"permision denied"];
- (void)promiseError:(RCTPromiseRejectBlock)reject message:(NSString *)message  {
  reject(@"", message, nil);
}

@end
