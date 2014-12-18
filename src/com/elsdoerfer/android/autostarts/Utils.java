package com.elsdoerfer.android.autostarts;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class Utils {
	static final String TAG = "Autostarts";
	private static Boolean isSELinuxEnforcing = null;

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

	public static class ShellFailedException extends Exception {} ;

	/**
	 * ShellFailedException indicates that the failure is the shell itself, not the command,
	 * and the caller thus should not retry alternative commands.

	 * @throws ShellFailedException
	 */
	static boolean runRootCommand(final String command, String[] env,
	                              int timeout, Shell.ShellContext context) throws ShellFailedException {

		RootTools.debugMode = true;

		// Workaround RootTools sending --context even if the su shell does not support it.
		// This code is copied from libsuperuser (we are not using it, because the way it does
		// async is sort of a mess.
		// https://github.com/Stericson/RootTools/issues/28
		Shell.ShellContext contextToUse = Shell.defaultContext;
		if ((context != null) && isSELinuxEnforcing()) {
			String display = version(false);
			String internal = version(true);

			// We only know the format for SuperSU v1.90+ right now
			if ((display != null) &&
					(internal != null) &&
					(display.endsWith("SUPERSU")) &&
					(Integer.valueOf(internal) >= 190)) {
				contextToUse = context;
			}
		}

		Shell shell;
		try {
			shell = RootTools.getShell(true, timeout, contextToUse, 0);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ShellFailedException();
		} catch (TimeoutException e) {
			e.printStackTrace();
			throw new ShellFailedException();
		} catch (RootDeniedException e) {
			e.printStackTrace();
			throw new ShellFailedException();
		}

		CommandCapture cmd = new CommandCapture(0, command);
		try {
			shell.add(cmd);
		} catch (IOException e) {
			throw new ShellFailedException();
		}

		// https://github.com/Stericson/RootTools/issues/10
		while (true) {
			if (cmd.isFinished())
				break;
			sleep(100);
		}

		return (cmd.getExitCode() == 0);
	}

	/**
	 * Sleep for a while; without dealing with the exception.
	 */
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * http://stackoverflow.com/a/25379180/15677
	 */
	public static boolean containsIgnoreCase(String src, String what) {
		if (src == null)
			return false;

		final int length = what.length();
		if (length == 0)
			return true; // Empty string is contained

		final char firstLo = Character.toLowerCase(what.charAt(0));
		final char firstUp = Character.toUpperCase(what.charAt(0));

		for (int i = src.length() - length; i >= 0; i--) {
			// Quick check before calling the more expensive regionMatches() method:
			final char ch = src.charAt(i);
			if (ch != firstLo && ch != firstUp)
				continue;

			if (src.regionMatches(true, i, what, 0, length))
				return true;
		}

		return false;
	}

	/**
	 * From libsuperuser.
	 *
	 * Detect if SELinux is set to enforcing, caches result
	 *
	 * @return true if SELinux set to enforcing, or false in the case of
	 *         permissive or not present
	 */
	public static synchronized boolean isSELinuxEnforcing() {
		if (isSELinuxEnforcing == null) {
			Boolean enforcing = null;

			// First known firmware with SELinux built-in was a 4.2 (17)
			// leak
			if (android.os.Build.VERSION.SDK_INT >= 17) {
				// Detect enforcing through sysfs, not always present
				if (enforcing == null) {
					File f = new File("/sys/fs/selinux/enforce");
					if (f.exists()) {
						try {
							InputStream is = new FileInputStream("/sys/fs/selinux/enforce");
							try {
								enforcing = (is.read() == '1');
							} finally {
								is.close();
							}
						} catch (Exception e) {
						}
					}
				}

				// 4.4+ builds are enforcing by default, take the gamble
				if (enforcing == null) {
					enforcing = (android.os.Build.VERSION.SDK_INT >= 19);
				}
			}

			if (enforcing == null) {
				enforcing = false;
			}

			isSELinuxEnforcing = enforcing;
		}
		return isSELinuxEnforcing;
	}

	/**
	 * From libsuperuser.
	 *
	 * <p>
	 * Detects the version of the su binary installed (if any), if supported
	 * by the binary. Most binaries support two different version numbers,
	 * the public version that is displayed to users, and an internal
	 * version number that is used for version number comparisons. Returns
	 * null if su not available or retrieving the version isn't supported.
	 * </p>
	 * <p>
	 * Note that su binary version and GUI (APK) version can be completely
	 * different.
	 * </p>
	 * <p>
	 * This function caches its result to improve performance on multiple
	 * calls
	 * </p>
	 *
	 * @param internal Request human-readable version or application
	 *            internal version
	 * @return String containing the su version or null
	 */
	private static String[] suVersion = new String[] {
			null, null
	};
	public static synchronized String version(boolean internal) {
		int idx = internal ? 0 : 1;
		if (suVersion[idx] == null) {
			String version = null;

			// Replace libsuperuser:Shell.run with manual process execution
			Process process;
			try {
				process = Runtime.getRuntime().exec(internal ? "su -V" : "su -v", null);
				process.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}

			// From libsuperuser:StreamGobbler
			List<String> stdout = new ArrayList<String>();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					stdout.add(line);
				}
			} catch (IOException e) {
			}
			// make sure our stream is closed and resources will be freed
			try {
				reader.close();
			} catch (IOException e) {
			}

			process.destroy();

			List<String> ret = stdout;
			
			if (ret != null) {
				for (String line : ret) {
					if (!internal) {
						if (line.contains(".")) {
							version = line;
							break;
						}
					} else {
						try {
							if (Integer.parseInt(line) > 0) {
								version = line;
								break;
							}
						} catch (NumberFormatException e) {
						}
					}
				}
			}

			suVersion[idx] = version;
		}
		return suVersion[idx];
	}

}
