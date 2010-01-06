#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "com_elsdoerfer_android_autostarts_NativeTask.h"

JNIEXPORT jint JNICALL Java_com_elsdoerfer_android_autostarts_NativeTask_runCommand
  (JNIEnv *env, jclass class, jstring command)
{
  const char *commandString;
  commandString = (*env)->GetStringUTFChars(env, command, 0);
  int exitcode = system(commandString); 
  (*env)->ReleaseStringUTFChars(env, command, commandString);  
  return (jint)exitcode;
}
