package com.elsdoerfer.android.autostarts;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class Utils {
	/**
	 * It's unbelievable how difficult it is in Java to read a stupid
	 * stream into a string.
	 *
	 * From:
	 * 	 http://stackoverflow.com/questions/309424/in-java-how-do-a-read-an-input-stream-in-to-a-string
	 */
	static String readStream(InputStream stream) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(stream, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read>0)
				out.append(buffer, 0, read);
		} while (read>=0);
		return out.toString();
	}

	/**
	 * Stupid Java's LinkedHashMap has no indexOf() method.
	 */
	static int getHashMapIndex(LinkedHashMap<?, ?> map, Object search) {
		Set<?> keys = map.keySet();
		Iterator<?> i = keys.iterator();
		Object curr;
		int count = -1;
		do {
			curr = i.next();
			count++;
			if (curr.equals(search))
				return count;
		}
		while (i.hasNext());
		return -1;
	}

	/**
	 * Determine if the device is rooted.
	 *
	 * This is based on the approach from android-wifi-tether.
	 *
	 * Note that the emulator and ADP1 device both have a su binary in
	 * /system/xbin/su, but it doesn't allow apps to use it (su app_29
	 * $ su su: uid 10029 not allowed to su).
	 */
	static boolean deviceHasRoot() {
    	File su = new File("/system/bin/su");
    	return su.exists();
	}

	/*
	 * Running an app through root isn't even that straightforward as
	 * it would seem. Here's some issues we've run into so far, and which
	 * we try to workaround here:
	 *
	 *  1) The Superuser Whitelist application most rooted devices
	 *     use identifies the command based on arguments. If we
	 *     were to just call su -c "pm xyz", the user would need
	 *     to confirm every single call; "Allows allow" would be
	 *     useless.
	 *
	 *  2) Rarely, a devices seems to have a su-executable that uses a
	 *     different argument syntax (`su -c "command args"` vs.
	 *     `su -c command args`).
	 *
	 *  3) Some ROMs have their "su" in a non-standard location, like
	 *     Archos in /data/bin, and it's not on the path either (this
	 *     is because they don't have write-access to /system yet).
	 *
	 *  4) Some custom ROMs contain what seems to be a kernel bug,
	 *     in which su/sh?, when run outside of the system shell,
	 *     cannot access certain paths. The error is "pm: not found".
	 *     This happens even though the file does exists, and runs
	 *     just fine from the shell.
	 *
	 * The common approach chosen by most root apps seems to be to
	 * run "su" and pipe commands into it. This will solve at least
	 * (1) and (2). We'll have to see about (4).
	 *
	 * (3) we still need to solve.
	 */
	static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			Log.d(ListActivity.TAG, "Running '"+command+"' as root");

			process = runWithEnv("su", null);
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command+"\n");
			os.writeBytes("echo \"rc:\" $?\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();

			Log.d(ListActivity.TAG, "Process returned with "+process.exitValue());
			Log.d(ListActivity.TAG, "Process stdout was: "+
				Utils.readStream(process.getInputStream())+
				"; stderr: "+Utils.readStream(process.getErrorStream()));

			// In order to consider this a success, we require to
			// things: a) a proper exit value, and ...
			if (process.exitValue() != 0)
				return false;

			return true;

		} catch (FileNotFoundException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		} catch (IOException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		} catch (InterruptedException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		}
		finally {
			if (os != null)
				try { os.close(); }
				catch (IOException e) { throw new RuntimeException(e); }
			if (process != null)
				process.destroy();
		}
	}

	/**
	 * This code is adapted from java.lang.ProcessBuilder.start().
	 *
	 * The problem is that Android doesn't allow us to modify the
	 * map returned by ProcessBuilder.environment(), even though the
	 * docstring indicates that it should. This is because it simply
	 * returns the SystemEnvironment object that System.getenv() gives
	 * us. The relevant portion in the source code is marked as
	 * "// android changed", so presumably it's not the case in the
	 * original version of the Apache Harmony project.
	 *
	 * Note that simply passing the environment variables we want
	 * to Process.exec won't be good enough, since that would override
	 * the environment we inherited completely.
	 *
	 * We needed to be able to set a CLASSPATH environment variable for
	 * our new process in order to use the "app_process" command directly.
	 * Note: "app_process" takes arguments passed on to the Dalvik VM as
	 * well; this might be an alternative way to set the class path.
	 */
	public static Process runWithEnv(String command, String[] customEnv) throws IOException {
		Map<String, String> environment = System.getenv();
	    String[] envArray = new String[environment.size()+
	                                   (customEnv != null ? customEnv.length : 0)];
	    int i = 0;
	    for (Map.Entry<String, String> entry : environment.entrySet())
	        envArray[i++] = entry.getKey() + "=" + entry.getValue();
	    if (customEnv != null)
		    for (String entry : customEnv)
		        envArray[i++] = entry;
	    Process process = Runtime.getRuntime().exec(command, envArray);
	    return process;
	}
}
