//
//  PhotoUtil.h
//  NativePhoto
//
//  Created by Eric Loew on 5/20/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#ifndef PhotoUtil_h
#define PhotoUtil_h

#import <React/RCTBridgeModule.h>
#import <UIKit/UIKit.h>

@interface PhotoUtil : NSObject <RCTBridgeModule, UIImagePickerControllerDelegate, UINavigationControllerDelegate>

@end
#endif
