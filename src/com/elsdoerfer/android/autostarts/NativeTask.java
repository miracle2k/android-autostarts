package com.elsdoerfer.android.autostarts;

import android.util.Log;

public class NativeTask {

	public static final String MSG_TAG = "Autostarts -> NativeTask";

	static {
        try {
            Log.i(MSG_TAG, "Trying to load libNativeTask.so");
            System.load("/data/data/com.elsdoerfer.android.autostarts/lib/libNativeTask.so");
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e(MSG_TAG, "Could not load libNativeTask.so");
        }
    }
    public static native int runCommand(String command);
}
