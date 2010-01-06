#include <jni.h>

#ifndef _Included_com_elsdoerfer_android_autostarts_NativeTask
#define _Included_com_elsdoerfer_android_autostarts_NativeTask
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_elsdoerfer_android_autostarts_NativeTask
 * Method:    runCommand
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_android_tether_system_NativeTask_runCommand
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
