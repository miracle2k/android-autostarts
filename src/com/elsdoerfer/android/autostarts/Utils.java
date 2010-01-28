package com.elsdoerfer.android.autostarts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

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
}
