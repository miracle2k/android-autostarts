package com.elsdoerfer.android.autostarts;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * This can be compared to android.content.pm.ComponentInfo,
 * except that we are forced to parse the package manifest files
 * ourselves, so we don't use the classes in android.content.pm.
 */
class ComponentInfo implements Parcelable {
	// These identify the component
	public String packageName;
	public String componentName;
	//  The receivers this component uses.
	public IntentFilterInfo[] intentFilters;

	// This is peripheral data
	String packageLabel;
	String componentLabel;
	public Drawable icon;
	public boolean isSystem;
	public int priority;
	public boolean defaultEnabled;
	public int currentEnabledState;

	ComponentInfo() {
	}

	@Override
	public String toString() {
		return String.format("%s/%s", packageName, componentName);
	}

	@Override
    public int hashCode() {
		return this.packageName.hashCode() ^ this.componentName.hashCode();
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
		switch (currentEnabledState) {
		case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
			return true;
		case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
			return false;
		case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
			return defaultEnabled;
		default:
			throw new RuntimeException(
					"Not a valid enabled state: "+currentEnabledState);
		}
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(packageName);
		dest.writeString(componentName);
		dest.writeString(packageLabel);
		dest.writeString(componentLabel);
		dest.writeInt(priority);
		dest.writeInt(isSystem ? 1 : 0);
		dest.writeInt(defaultEnabled ? 1 : 0);
		dest.writeInt(currentEnabledState);
	}

	public static final Parcelable.Creator<ComponentInfo> CREATOR
	= new Parcelable.Creator<ComponentInfo>()
	{
		public ComponentInfo createFromParcel(Parcel in) {
			return new ComponentInfo(in);
		}

		public ComponentInfo[] newArray(int size) {
			return new ComponentInfo[size];
		}
	};

	private ComponentInfo(Parcel in) {
		packageName = in.readString();
		componentName = in.readString();
		packageLabel = in.readString();
		componentLabel = in.readString();
		priority = in.readInt();
		isSystem = in.readInt() == 1;
		defaultEnabled = in.readInt() == 1;
		currentEnabledState = in.readInt();
	}

	/**
	 * A single intent filter applied to a Component. The Android
	 * PackageManager API does not expose this info, which is why
	 * there is no corresponding class in the android.content.pm namespace.
	 *
	 * Effectively, this represents a single "event" that we show to the
	 * user in the GUI, and which we allow him to enable/disable. However,
	 * because a single component can use multiple filters, the user will
	 * usually be unaware that what really happens is that the filter's
	 * *component* will be disabled.
	 *
	 * That means a single user action may in fact disable multiple
	 * "events". By modeling our classes to appropriately reflect this
	 * architecture and actually storing the current "enabled state" in
	 * the Component class, handling multi-intent components correctly
	 * is almost automatic.
	 */
	static class IntentFilterInfo {
		ComponentInfo componentInfo;
		String action;
		int priority;

		public IntentFilterInfo(ComponentInfo componentInfo, String action,
				int priority) {
			this.componentInfo = componentInfo;
			this.action = action;
			this.priority = priority;
		}

		@Override
	    public int hashCode() {
			return this.componentInfo.hashCode() ^ this.action.hashCode();
		}
	}
}

