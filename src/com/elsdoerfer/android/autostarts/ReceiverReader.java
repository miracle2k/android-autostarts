package com.elsdoerfer.android.autostarts;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.elsdoerfer.android.autostarts.db.ComponentInfo;
import com.elsdoerfer.android.autostarts.db.IntentFilterInfo;
import com.elsdoerfer.android.autostarts.db.PackageInfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.util.Log;


/**
 * Load the broadcast receivers installed by applications.
 *
 * Android provides some introspection capabilities through it's
 * PackageManager API, but this is insufficient:
 *
 * 1) Looping through the list of installed packages and collecting
 *    all receivers doesn't expose to us the intents a receiver has
 *    registered for. See the unimplemented
 *    PackageManager.GET_INTENT_FILTERS, and the following URLs:
 *       http://groups.google.com/group/android-developers/browse_thread/thread/4502827143ea9b20
 *       http://groups.google.com/group/android-developers/browse_thread/thread/ef0e4b390552f2c/
 *
 * 2) Using PackageManager.queryBroadcastReceivers() to find all installed
 *    receivers works, but has numerous restrictions:
 *        * We need an internal list of actions that we support and
 *          query for.
 *        * Disabled components are never returned.
 *        * Receivers who's intent filters match certain data are only
 *          returned when our query matches the receiver's filters.
 *    It is possible to work around those issues, for example by
 *    remembering what components have been disabled by the user, and
 *    we used to do this in the past, but in the end it's a very
 *    restricting approach.
 *
 * Fortunately, it's relatively simple to parse the AndroidManifest.xml
 * files of every package ourselves and extract the data we need.
 *
 * Parts of this were adapted from ManifestExplorer:
 * 		https://www.isecpartners.com/manifest_explorer.html
 */
public class ReceiverReader {

	private static final boolean LOGV = ListActivity.LOGV;
	private static final String TAG = Utils.TAG;

	// From com.android.sdklib.SdkConstants.NS_RESOURCES.
	private final static String SDK_NS_RESOURCES =
		"http://schemas.android.com/apk/res/android";

	public interface OnLoadProgressListener {
		public void onProgress(ArrayList<IntentFilterInfo> currentState, float progress);
	}

	private static enum ParserState { Unknown, InManifest,
		InApplication, InReceiver, InIntentFilter, InAction }

	private final Context mContext;
	private final PackageManager mPackageManager;
	private XmlResourceParser mCurrentXML;
	private Resources mCurrentResources;
	private OnLoadProgressListener mOnLoadProgressListener;
	private ArrayList<IntentFilterInfo> mResult;

	// Parser state flags
	android.content.pm.PackageInfo mAndroidPackage = null;
	String mCurrentApplicationLabel = null;
	PackageInfo mCurrentPackage = null;
	ParserState mCurrentState = ParserState.Unknown;
	ComponentInfo mCurrentComponent = null;
	int mCurrentFilterPriority = 0;

	/**
	 * Constructor.
	 */
	public ReceiverReader(Context context,
			OnLoadProgressListener progressListener) {
		mContext = context;
		mPackageManager = mContext.getPackageManager();
		mOnLoadProgressListener = progressListener;
	}

	/**
	 * Main method to make this class go ahead and do it's job.
	 */
	public ArrayList<IntentFilterInfo> load() {
		mResult = new ArrayList<IntentFilterInfo>();

		List<android.content.pm.PackageInfo> packages =
			mPackageManager.getInstalledPackages(PackageManager.GET_DISABLED_COMPONENTS);
		int packageCount = packages.size();
		for (int i=0; i<packageCount; i++)
		{
			android.content.pm.PackageInfo p = packages.get(i);

			if (LOGV) Log.v(TAG, "Processing package "+p.packageName);
			parsePackage(p);

			// Publish an update after every package
			if (mOnLoadProgressListener != null) {
				// It's important that we send out a copy there, or we'll
				// can run into crashes both inside our ListAdapter filtering
				// code, where we are iterating over a list that can be
				// changed by the thread in the background simultaneously
				// (ConcurrentModificationException), and the core Android
				// ListAdapter stuff itself which complains about the
				// data having changed without notifyDataSetChanged being
				// called. Note that this is a shallow copy, the actual
				// objects in both lists remain the same. This is important
				// so that when a receiver status is toggled while we are
				// still loading, it's changed attributes are not reset by
				// the next progress update.
				@SuppressWarnings("unchecked")
				ArrayList<IntentFilterInfo> copy = (ArrayList<IntentFilterInfo>) mResult.clone();
				mOnLoadProgressListener.onProgress(copy, i/(float)packageCount);
			}
		}

		return mResult;
	}

	private void parsePackage(android.content.pm.PackageInfo p) {
		// Open the manifest file
		XmlResourceParser xml = null;
		Resources resources = null;
		try {
			Context scannedAppContext = mContext.createPackageContext(p.packageName, 0);
			AssetManager assets = scannedAppContext.getAssets();
			xml = openManifest(scannedAppContext, assets);
			resources = new Resources(assets, mContext.getResources().getDisplayMetrics(), null);
		} catch (IOException e) {
			Log.e(TAG, "Unable to open manifest or resources for "+p.packageName, e);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to open manifest or resources for "+p.packageName, e);
		} catch (NullPointerException e) {
			// I've been seeing a lot of NullPointerException's in
			// "android.app.ApplicationContext.init", called by
			// createPackageContext(). Due to the help of a user it was
			// determined that the problem can be reproduced by removing
			// or renaming an application file in /data/app. That this is
			// going to cause unexpected problems - given. But why is
			// it happening in the first place on production phones?
			//
			// Anyway, as I said, this is happening a lot - a bunch of
			// stacktraces get sent my way every single day. This should
			// be working around it.
			Log.e(TAG, "Error processing "+p.packageName + " - most likely,"+
					" the createPackageContext() call failed; if so, your"+
					" application directory is somehow screwed up. Android" +
					" is probably to blame, since it shouldn't be happening. Skipping.", e);
		}

		if (xml == null)
			return;

		mAndroidPackage = p;
		mCurrentPackage = null;
		mCurrentXML = xml;
		mCurrentResources = resources;

		try {
			String tagName = null;
			mCurrentState = ParserState.Unknown;
			int eventType = xml.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					tagName = xml.getName();
					if (tagName.equals("manifest"))
						startManifest();
					else if (tagName.equals("application"))
						startApplication();
					else if (tagName.equals("receiver"))
						startReceiver();
					else if (tagName.equals("intent-filter"))
						startIntentFilter();
					else if (tagName.equals("action"))
						startAction();
					break;

				case XmlPullParser.END_TAG:
					tagName = xml.getName();
					if (tagName.equals("manifest"))
						endManifest();
					else if (tagName.equals("application"))
						endApplication();
					else if (tagName.equals("receiver"))
						endReceiver();
					else if (tagName.equals("intent-filter"))
						endIntentFilter();
					else if (tagName.equals("action"))
						endAction();
					break;
				}
				eventType = xml.nextToken();
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Unable to process manifest for "+p.packageName, e);
		} catch (IOException e) {
			Log.e(TAG, "Unable to process manifest for "+p.packageName, e);
		}
		finally {
			mCurrentXML = null;
			mCurrentResources = null;
		}
	}

	/**
	 * Open AndroidManifest.xml file stored in apk,
	 * working around skin manifest bug on some Xperia devices
	 */
	private XmlResourceParser openManifest(Context scannedAppContext, AssetManager assets) throws IOException {
		try {
			// Use reflection to avoid VerifyError on old devices, this is equivalent to:
			//String packageResourcePath = scannedAppContext.getPackageResourcePath();
			String packageResourcePath = (String) Context.class.getMethod("getPackageResourcePath")
					.invoke(scannedAppContext);

			// getCookieName is @hide method, it returns name of apk file for given asset cookie
			Method getCookieName = AssetManager.class.getMethod("getCookieName", int.class);

			// "android" package has no resource path, use hardcoded path,
			// If we won't find it, we'll open manifest in non-workaround way
			if (packageResourcePath == null && scannedAppContext.getPackageName().equals("android")) {
				packageResourcePath = "/system/framework/framework-res.apk";
			}

			// This loop shouldn't reach 20,
			// getCookieName will throw IndexOutOfBoundsException when passed illegal cookie,
			// but we expect to find right apk and return earlier.
			for (int i = 1; i < 20; i++) {
				if (packageResourcePath.equals(getCookieName.invoke(assets, i))) {
					return assets.openXmlResourceParser(i, "AndroidManifest.xml");
				}
			}
		} catch (Exception ignored) {
			// Something went wrong with workaround, ignore and use normal method
		}

		// Normal way of opening manifest, used if workaround above fails
		return assets.openXmlResourceParser("AndroidManifest.xml");
	}

	void startManifest() {
		if (mCurrentState == ParserState.Unknown)
			mCurrentState = ParserState.InManifest;
	}

	void endManifest() {
		if (mCurrentState == ParserState.InManifest)
			mCurrentState = ParserState.Unknown;
	}

	void startApplication() {
		if (mCurrentState != ParserState.InManifest)
			return;
		mCurrentState = ParserState.InApplication;
		mCurrentApplicationLabel = getAttr("label");
	}

	void endApplication() {
		if (mCurrentState == ParserState.InApplication) {
			mCurrentState = ParserState.InManifest;
			mCurrentApplicationLabel = null;
		}
	}

	void startReceiver() {
		if (mCurrentState != ParserState.InApplication)
			return;

		mCurrentState = ParserState.InReceiver;

		// Build the component name. We need to do some normalization here,
		// since we can get the original string the dev. put into his XML.
		// Our current logic is: If the component name starts with a dot,
		// or doesn't contain one, we assume a relative name and prepend the
		// package name. Otherwise, we consider the component name to be
		// absolute already.
		String componentName = getAttr("name");
		if (componentName == null) {
			Log.e(TAG, "A receiver in "+mAndroidPackage.packageName+" has no name.");
			componentName = "(no-name)";
		}
		else if (componentName.startsWith("."))
			componentName = mAndroidPackage.packageName + componentName;
		else if (!componentName.contains("."))
			componentName = mAndroidPackage.packageName + "." + componentName;

		// Note that we specifically delay creating the package object
		// until we are sure there are actually receivers in this package.
		if (mCurrentPackage == null) {
			mCurrentPackage = new PackageInfo(mAndroidPackage);
			mCurrentPackage.isSystem = isSystemApp(mAndroidPackage);
			mCurrentPackage.packageLabel = mCurrentApplicationLabel;
			// TODO: Traceview says this takes 9% of the total load
			// time. We could move it to the drawing code (load only
			// once the user actually sees an icon), but that would
			// slow down the list view usage. One option possibly would
			// be to load it on-demand, but do that again in a thread.
			mCurrentPackage.icon =
				mAndroidPackage.applicationInfo.loadIcon(mPackageManager);
		}

		mCurrentComponent = new ComponentInfo();
		mCurrentComponent.packageInfo = mCurrentPackage;
		mCurrentComponent.componentName = componentName;
		mCurrentComponent.componentLabel = getAttr("label");
		mCurrentComponent.defaultEnabled = !(getAttr("enabled") == "false");;
		mCurrentComponent.currentEnabledState =
		    mPackageManager.getComponentEnabledSetting(
			    new ComponentName(mCurrentPackage.packageName,
			    		mCurrentComponent.componentName));
	}

	void endReceiver() {
		if (mCurrentState == ParserState.InReceiver) {
			mCurrentComponent = null;
			mCurrentState = ParserState.InApplication;
		}
	}

	void startIntentFilter() {
		if (mCurrentState != ParserState.InReceiver)
			 return;

		mCurrentState = ParserState.InIntentFilter;

		String priorityRaw = getAttr("priority");
		if (priorityRaw != null)
			try {
				mCurrentFilterPriority = Integer.parseInt(priorityRaw);
			} catch (NumberFormatException e) {
				Log.w(TAG, "Unable to parse priority value "+
						"for receiver "+mCurrentComponent.componentName+
						" in package "+mCurrentPackage.packageName+": "+priorityRaw);
			}
			if (LOGV && mCurrentFilterPriority != 0)
				Log.v(TAG, "Receiver "+mCurrentComponent.componentName+
						" in package "+mCurrentPackage.packageName+" has "+
				"an intent filter with priority != 0");
    }

	void endIntentFilter() {
		if (mCurrentState == ParserState.InIntentFilter) {
			mCurrentState = ParserState.InReceiver;
			mCurrentFilterPriority = 0;
		}
	}

	void startAction() {
		if (mCurrentState != ParserState.InIntentFilter)
			return;

		mCurrentState = ParserState.InAction;

		// A component name is missing, we can't proceed.
		if (mCurrentComponent == null)
			return;

		String action = getAttr("name");
		if (action == null) {
			Log.w(TAG, "Receiver "+mCurrentComponent.componentName+
					   " of package "+mCurrentPackage.packageName+" has "+
			           "action without name");
			return;
		}

		// Add this receiver to the result
		IntentFilterInfo filter = new IntentFilterInfo(
				mCurrentComponent, action, mCurrentFilterPriority);
		mResult.add(filter);
	}

	void endAction() {
		if (mCurrentState == ParserState.InAction)
			mCurrentState = ParserState.InIntentFilter;
	}

	/**
	 * True if this app is installed on the system partition.
	 */
	static boolean isSystemApp(android.content.pm.PackageInfo p) {
		// You'd think that it would be possible to determine the
		// system status of packages that do not have a application,
		// as rare as that may be, but alas, it doesn't look like it.
		// Of course, in those cases there'd be no receivers for us
		// either, so we don't really care about this case.
		return ((p.applicationInfo != null)  && (
				ApplicationInfo.FLAG_SYSTEM & p.applicationInfo.flags)
				== ApplicationInfo.FLAG_SYSTEM);
	}

	/**
	 * Returns the requested attribute value, or null.
	 *
	 * Ensures that we only read from the Android namespace, and resolves
	 * resource identifiers if necessary.
	 */
	private String getAttr(String attributeName) {
		String value = mCurrentXML.getAttributeValue(SDK_NS_RESOURCES, attributeName);

		// In some, rarer cases (example: com.mxtech.videoplayer.MediaButtonReceiver), the
		// xml attributes seem to be resource encoded such that it is not possibly to query
		// by name. In fact, getAttributeName(0) returns an empty string. In such cases, the
		// attribute rather tha a name seems to be a resource id (getAttributeNameResource),
		// pointing to a resource in the framework.
		// We have to look at all such attributes, resolve the resource to a name, ad then
		// we can check *that*.
		// See:
		//   https://code.google.com/p/android-apktool/issues/detail?id=512
		//   https://github.com/iBotPeaches/Apktool/commit/e126a51b4bb8991042b48ec5bf916f396e75f6f0#diff-0e7a43a489bb79777d002aed6191d459R317
		if (value == null) {
			for (int i=0; i<mCurrentXML.getAttributeCount(); i++) {
				if (!mCurrentXML.getAttributeName(i).equals(""))
					// Normally, both the name and resource return values. Unless the
					// attribute name is empty we don't need this code and we can trust
					// the check above to find the attribute.
					continue;

				int res = mCurrentXML.getAttributeNameResource(i);
				if (res != 0) {
					String sName = mCurrentResources.getResourceEntryName(res);
					if (sName.equals(attributeName)) {
						value = mCurrentXML.getAttributeValue(i);
						break;
					}
				}
			}
		}

		// TODO: It's possible to use getAttributeResourceValue and check for
		// default value return rather than parsing the @ ourselves. Is it faster?
		return resolveValue(value, mCurrentResources);
	}

	/**
	 * Return the value, resolving it through the provided resources if
	 * it appears to be a resource ID. Otherwise just returns what was
	 * provided.
	 */
	private String resolveValue(String in, Resources r) {
		if (in == null || !in.startsWith("@") || r == null)
			return in;
		try {
			int num = Integer.parseInt(in.substring(1));
			return r.getString(num);
		} catch (NumberFormatException e) {
			return in;
		} catch (NotFoundException e) {
			Log.w(TAG, "Unable to resolve resource "+in, e);
			return in;
		}
		// ManifestExplorer used this catch-all, not sure why. Seems to
		// work fine without it, for now. Note that we added the
		// NotFoundException catch ourselves.
		//catch (RuntimeException e) {
		// formerly noted errors here, but simply not resolving works better
		//	return in;
		//}
	}
}
