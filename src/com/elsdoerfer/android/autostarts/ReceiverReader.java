package com.elsdoerfer.android.autostarts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
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
	private static final String TAG = ListActivity.TAG;

	// From com.android.sdklib.SdkConstants.NS_RESOURCES.
	private final static String SDK_NS_RESOURCES = "http://schemas.android.com/apk/res/android";

	public interface OnLoadProgressListener {
		public void onProgress(ArrayList<ActionWithReceivers> currentState);
	}

	private static enum ParserState { Unknown, InManifest,
		InApplication, InReceiver, InIntentFilter, InAction }

	private final Context mContext;
	private final PackageManager mPackageManager;
	private XmlResourceParser mCurrentXML;
	private Resources mCurrentResources;
	private OnLoadProgressListener mOnLoadProgressListener;

	public ReceiverReader(Context context,
			OnLoadProgressListener progressListener) {
		mContext = context;
		mPackageManager = mContext.getPackageManager();
		mOnLoadProgressListener = progressListener;
	}

	public ArrayList<ActionWithReceivers> load() {
		ArrayList<ActionWithReceivers> receiversByIntent =
			new ArrayList<ActionWithReceivers>();

		for (PackageInfo p : mPackageManager.getInstalledPackages(
				PackageManager.GET_DISABLED_COMPONENTS))
		{
			if (LOGV) Log.v(TAG, "Processing package "+p.packageName);
			parsePackage(p, receiversByIntent);

			// Publish an update after every package
			if (mOnLoadProgressListener != null) {
				sortResult(receiversByIntent);

				// It's important that we send out a copy here, or we'll can run into
				// crashes both inside our ListAdapter filtering code, where we are
				// iterating over a list that can be changed by the thread in the
				// background simultaneously (ConcurrentModificationException), and
				// the core Android ListAdapter stuff itself which complains about the
				// data having changed without notifyDataSetChanged being called.
				// Note that what is not copied are the "ReceiverData" objects itself.
				// This is important so that when a receiver status is toggled while
				// we are still loading, it's changed attributes are not reset, because
				// the ToggleTask modifies the single one instance that we have in both
				// the authoritative and the intermediate copies.
				ArrayList<ActionWithReceivers> copy = new ArrayList<ActionWithReceivers>();
				for (ActionWithReceivers action : receiversByIntent) {
					try {
						copy.add((ActionWithReceivers) action.clone());
					} catch (CloneNotSupportedException e) {
						throw new RuntimeException(e);
					}
				}
				mOnLoadProgressListener.onProgress(copy);
			}
		}

		sortResult(receiversByIntent);
		return receiversByIntent;
	}

	private void sortResult(ArrayList<ActionWithReceivers> receiverList) {
		// Sort both groups and children. Groups are sorted by the
		// order in which we define our known intents, children
		// are simply sorted alphabetically.
		Collections.sort(receiverList, new Comparator<ActionWithReceivers>() {
			public int compare(ActionWithReceivers object1,
					ActionWithReceivers object2) {
				int idx1 = Utils.getHashMapIndex(Actions.MAP, object1.action);
				int idx2 = Utils.getHashMapIndex(Actions.MAP, object2.action);
				// Make sure that unknown intents (-1) are sorted at the bottom.
				if (idx1 == -1 && idx2 == -1)
					return object1.action.compareTo(object2.action);
				else if (idx1 == -1)
					return +1;
				else if (idx2 == -1)
					return -1;
				else
					return ((Integer)idx1).compareTo(idx2);
			}
		});
		for (ActionWithReceivers action : receiverList)
			Collections.sort(action.receivers);
	}

	private void parsePackage(PackageInfo p, ArrayList<ActionWithReceivers> result) {
		// Open the manifest file
		XmlResourceParser xml = null;
		Resources resources = null;
		try {
			AssetManager assets = mContext.createPackageContext(p.packageName, 0).getAssets();
			xml = assets.openXmlResourceParser("AndroidManifest.xml");
			resources = new Resources(assets, mContext.getResources().getDisplayMetrics(), null);
		} catch (IOException e) {
			Log.e(TAG, "Unable to open manifest or resources for "+p.packageName, e);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to open manifest or resources for "+p.packageName, e);
		}

		if (xml == null)
			return;

		mCurrentXML = xml;
		mCurrentResources = resources;

		try {
			String tagName = null;
			String currentComponentName = null;
			String currentApplicationLabel = null;
			boolean currentComponentEnabled = true;
			int currentFilterPriority = 0;
			ParserState state = ParserState.Unknown;

			int eventType = xml.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					tagName = xml.getName();
					if (tagName.equals("manifest") && state == ParserState.Unknown)
						state = ParserState.InManifest;
					else if (tagName.equals("application") && state == ParserState.InManifest) {
						state = ParserState.InApplication;
						currentApplicationLabel = getAttr("label");
					}
					else if (tagName.equals("receiver") && state == ParserState.InApplication)
					{
						state = ParserState.InReceiver;

						currentComponentEnabled =
							!(getAttr("enabled") == "false");
						// Build the component name. We need to do some
						// normalization here, since we can get the
						// original string the dev. put into his XML.
						// Our current logic is: If the component name
						// starts with a dot, or doesn't contain one,
						// we assume a relative name and prepend the
						// package name. Otherwise, we consider the
						// component name to be absolute already.
						currentComponentName = getAttr("name");
						if (currentComponentName == null)
							Log.e(TAG, "A receiver in "+p.packageName+" has no name.");
						else if (currentComponentName.startsWith("."))
							currentComponentName = p.packageName + currentComponentName;
						else if (!currentComponentName.contains("."))
							currentComponentName = p.packageName + "." + currentComponentName;
					}
					else if (tagName.equals("intent-filter") && state == ParserState.InReceiver)
					{
						state = ParserState.InIntentFilter;

						String priorityRaw = getAttr("priority");
						if (priorityRaw != null)
							try {
								currentFilterPriority = Integer.parseInt(priorityRaw);
							} catch (NumberFormatException e) {
								Log.w(TAG, "Unable to parse priority value "+
										"for receiver "+currentComponentName+
										" in package "+p.packageName+": "+priorityRaw);
							}
							if (LOGV && currentFilterPriority != 0)
								Log.v(TAG, "Receiver "+currentComponentName+
										" in package "+p.packageName+" has "+
								"an intent filter with priority != 0");
					}
					else if (tagName.equals("action") && state == ParserState.InIntentFilter)
					{
						state = ParserState.InAction;

						// A component name is missing, we can't proceed.
						if (currentComponentName == null)
							break;

						String action = getAttr("name");
						if (action == null) {
							Log.w(TAG, "Receiver "+currentComponentName+
									" of package "+p.packageName+" has "+
							"action without name");
							break;
						}

						// See if a record for this action exists,
						// otherwise create a new one.
						ActionWithReceivers record = null;
						for (ActionWithReceivers r : result) {
							if (r.action.equals(action)) {
								record = r;
								break;
							}
						}
						if (record == null) {
							record = new ActionWithReceivers(action);
							result.add(record);
						}

						// Add this receiver to the list
						ReceiverData data = new ReceiverData();
						data.action = action;
						data.componentName = currentComponentName;
						data.packageName = p.packageName;
						data.isSystem = isSystemApp(p);
						data.packageLabel =  currentApplicationLabel;
						data.componentLabel = getAttr("label");
						// TODO: Traceview says this takes 9% of the total load
						// time. We could move it to the drawing code (load only
						// once the user actually sees an icon), but that would
						// slow down the list view usage. One option possibly would
						// be to load it on-demand, but do that again in a thread.
						data.icon = p.applicationInfo.loadIcon(mPackageManager);
						data.priority = currentFilterPriority;
						data.defaultEnabled = currentComponentEnabled;
						data.currentEnabled = mPackageManager.getComponentEnabledSetting(
								new ComponentName(data.packageName, data.componentName));
						record.receivers.add(data);
					}
					break;

				case XmlPullParser.END_TAG:
					tagName = xml.getName();
					if (tagName.equals("action") && state == ParserState.InAction)
						state = ParserState.InIntentFilter;
					else if (tagName.equals("intent-filter") && state == ParserState.InIntentFilter) {
						state = ParserState.InReceiver;
						currentFilterPriority = 0;
					}
					else if (tagName.equals("receiver") && state == ParserState.InReceiver) {
						currentComponentName = null;
						currentComponentEnabled = true;
						state = ParserState.InApplication;
					}
					else if (tagName.equals("application") && state == ParserState.InApplication) {
						currentApplicationLabel = null;
						state = ParserState.InManifest;
					}
					else if (tagName.equals("manifest") && state == ParserState.InManifest)
						state = ParserState.Unknown;
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
	 * True if this app is installed on the system partition.
	 */
	static boolean isSystemApp(PackageInfo p) {
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
			// XXX: Investigate why this occurs on Milestone
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

	/**
	 * Represent a receiver for a single action.
	 */
	static class ReceiverData implements Comparable<ReceiverData>, Parcelable {

		// These identify the component
		public String packageName;
		public String componentName;

		// This is peripheral data
		String packageLabel;
		String componentLabel;
		public String action;
		public Drawable icon;
		public boolean isSystem;
		public int priority;
		public boolean defaultEnabled;
		public int currentEnabled;

		private ReceiverData(Parcel in) {
			packageName = in.readString();
			componentName = in.readString();
			packageLabel = in.readString();
			componentLabel = in.readString();
			action = in.readString();
			priority = in.readInt();
			isSystem = in.readInt() == 1;
			defaultEnabled = in.readInt() == 1;
			currentEnabled = in.readInt();
		}

		public ReceiverData() {}

		@Override
		public String toString() {
			return String.format("%s/%s for %s",
					packageName, componentName, action);
		}

		/**
		 * Return the best label we have.
		 */
		public String getAnyLabel() {
			if (componentLabel != null && !componentLabel.equals(""))
				return componentLabel;
			else if (packageLabel != null && !packageLabel.equals(""))
				return packageLabel;
			else
				return packageName;
		}

		/**
		 * Return a label identifying the app.
		 */
		public String getAppLabel() {
			if (packageLabel != null && !packageLabel.equals(""))
				return packageLabel;
			else
				return packageName;
		}

		/**
		 * Resolve the current and default "enabled" state.
		 */
		public boolean isCurrentlyEnabled() {
			if (currentEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
				return true;
			else if (currentEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
				return false;
			else if (currentEnabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
				return defaultEnabled;
			else
				throw new RuntimeException("Not a valid enabled state: "+currentEnabled);
		}

		@Override
		public boolean equals(Object o) {
			// The code merging the cached components with those found in
			// recovery relies on this method.
			// See also hashCode(), of course.
			if (!(o instanceof ReceiverData))
				return false;
			return (((ReceiverData)o).componentName.equals(componentName) &&
					((ReceiverData)o).packageName.equals(packageName));
		}

		@Override
		public int hashCode() {
			return (packageName+componentName).hashCode();
		}

		public int compareTo(ReceiverData another) {
			int result = ((Integer)priority).compareTo(((ReceiverData)another).priority);
			if (result != 0)
				return result;
			else
				return componentName.compareToIgnoreCase(
						((ReceiverData)another).componentName);
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(packageName);
			dest.writeString(componentName);
			dest.writeString(packageLabel);
			dest.writeString(componentLabel);
			dest.writeString(action);
			dest.writeInt(priority);
			dest.writeInt(isSystem ? 1 : 0);
			dest.writeInt(defaultEnabled ? 1 : 0);
			dest.writeInt(currentEnabled);
		}

		public static final Parcelable.Creator<ReceiverData> CREATOR
		= new Parcelable.Creator<ReceiverData>()
		{
			public ReceiverData createFromParcel(Parcel in) {
				return new ReceiverData(in);
			}

			public ReceiverData[] newArray(int size) {
				return new ReceiverData[size];
			}
		};
	}

	/**
	 * A particular action and a list of receivers that register for
	 * the action.
	 */
	static class ActionWithReceivers implements Cloneable {
		public String action;
		public ArrayList<ReceiverData> receivers;

		ActionWithReceivers(String action) {
			this.action = action;
			this.receivers = new ArrayList<ReceiverData>();
		}

		ActionWithReceivers(ActionWithReceivers clone) {
			this.action = clone.action;
			this.receivers = clone.receivers;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Object clone() throws CloneNotSupportedException {
			ActionWithReceivers clone = (ActionWithReceivers)super.clone();
			clone.receivers = (ArrayList<ReceiverData>) receivers.clone();
			return clone;

		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ActionWithReceivers)
				return action.equals(((ActionWithReceivers)o).action);
			else
				return action.equals(o);
		}

		@Override
		public int hashCode() { return action.hashCode(); }
	}
}
