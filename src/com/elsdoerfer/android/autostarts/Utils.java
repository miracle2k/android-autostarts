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
	 *  2) su (or the Superuser Whitelist app) seem to have a bug
	 *     that can cause them to freeze if "USB Debugging" is not
	 *     enabled. It doesn't happen in every scenario, but when it
	 *     does, it happens always.
	 *
	 *  3) Some custom ROMs contain what seems to be a kernel bug,
	 *     in which su/sh?, when run outside of the system shell,
	 *     cannot access certain paths. The error is "pm: not found".
	 *     This happens even though the file does exists, and runs
	 *     just fine from the shell.
	 *
	 * The common approach chosen by most root apps seems to be to
	 * run "su" and pipe commands into it. This will solve at least
	 * (1). It does't seem to affect (2). We'll have to see about (3).
	 */
	static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			Log.d(ListActivity.TAG, "Running '"+command+"' as root");

			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command+"\n");
			os.writeBytes("echo \"rc\" $?\n");
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
}
