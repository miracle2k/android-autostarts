package com.elsdoerfer.android.autostarts.db;

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
public class IntentFilterInfo {
	public ComponentInfo componentInfo;
	public String action;
	public int priority;

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
