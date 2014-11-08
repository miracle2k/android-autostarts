/**
 *
 */
package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.elsdoerfer.android.autostarts.db.ComponentInfo;
import com.elsdoerfer.android.autostarts.db.IntentFilterInfo;
import com.elsdoerfer.android.autostarts.db.PackageInfo;

import static com.elsdoerfer.android.autostarts.Utils.containsIgnoreCase;


/**
 * ListAdapter used by the ListActivity. Has it's own top-level file to
 * keep file sizes small.
 */
public class MyExpandableListAdapter extends BaseExpandableListAdapter {

	static final public int GROUP_BY_ACTION = 1;
	static final public int GROUP_BY_PACKAGE = 2;

	private ListActivity mActivity;
	private ArrayList<IntentFilterInfo> mDataAll;
	private GroupingImpl mGroupDisplay;
	private int mCurrentGrouping = GROUP_BY_ACTION;

	private boolean mHideSystemApps = false;
	private boolean mHideUnknownEvents = false;
	private boolean mShowChangedOnly = false;
	private String mTextFilter = "";

	private LayoutInflater mInflater;

	public MyExpandableListAdapter(ListActivity activity) {
		mActivity = activity;
		mInflater = (LayoutInflater) activity.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		setData(new ArrayList<IntentFilterInfo>());
	}

	public void setData(ArrayList<IntentFilterInfo> data) {
		mDataAll = data;
		rebuildGroupDisplay();
	}

	public void setGrouping(int groupMode) {
		if (mCurrentGrouping != groupMode) {
			mCurrentGrouping = groupMode;
			rebuildGroupDisplay();
		}
	}

	public int getGrouping() {
		return mCurrentGrouping;
	}

	private boolean checkAgainstFilters(IntentFilterInfo info) {
		ComponentInfo comp = info.componentInfo;

		// If a text filter is active, all other filters are ignored
		if (!mTextFilter.equals("")) {
			if (containsIgnoreCase(comp.componentLabel, mTextFilter))
				return true;
			if (containsIgnoreCase(comp.componentName, mTextFilter))
				return true;
			if (containsIgnoreCase(comp.packageInfo.packageLabel, mTextFilter))
				return true;
			if (containsIgnoreCase(comp.packageInfo.packageName, mTextFilter))
			return true;
			// Search comp.intentFilters too?
			return false;
		}

		if (mHideSystemApps && comp.packageInfo.isSystem)
			return false;
		if (mShowChangedOnly && comp.isCurrentlyEnabled() ==
			    comp.defaultEnabled)
			return false;
		if (mHideUnknownEvents && Utils.getHashMapIndex
				(Actions.MAP, info.action) == -1)
			return false;
		return true;
	};

	/**
	 * Rebuild the grouping-mode specific rendering object. This
	 * re-applies the filters.
	 *
	 * TODO: Add a way to init all filters (setFilterFOO calls) without
	 * updating the data once for every filter option. Simplest way:
	 * generate this on demand?
	 */
	private void rebuildGroupDisplay() {
		switch (mCurrentGrouping) {
		case GROUP_BY_ACTION:
			mGroupDisplay = new GroupByActionImpl(mDataAll, this);
			break;
		case GROUP_BY_PACKAGE:
			mGroupDisplay = new GroupByPackageImpl(mDataAll, this);
		}
	}

	public Object getChild(int groupPosition, int childPosition) {
		return mGroupDisplay.getChild(groupPosition, childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return mGroupDisplay.getChildId(groupPosition, childPosition);
	}

	public int getChildrenCount(int groupPosition) {
		return mGroupDisplay.getChildrenCount(groupPosition);
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		return mGroupDisplay.getChildView(groupPosition, childPosition,
				isLastChild, convertView, parent);
	}

	public Object getGroup(int groupPosition) {
		return mGroupDisplay.getGroup(groupPosition);
	}

	public int getGroupCount() {
		return mGroupDisplay.getGroupCount();
	}

	public long getGroupId(int groupPosition) {
		return mGroupDisplay.getGroupId(groupPosition);
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {
		return mGroupDisplay.getGroupView(groupPosition, isExpanded,
				convertView, parent);
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public boolean hasStableIds() {
		return true;
	}

	/**
	 * Return true if any filters are active.
	 */
	public boolean isFiltered() {

		return mHideSystemApps || mShowChangedOnly || mHideUnknownEvents || !mTextFilter.equals("");
	}

	/**
	 * Allow owner to hide (and show) the system applications.
	 *
	 * Returns True if the list is filtered.
	 *
	 * Expects the caller to also call notifyDataSetChanged(), if
	 * necessary.
	 */
	public boolean toggleFilterSystemApps() {
	    setFilterSystemApps(!mHideSystemApps);
		return mHideSystemApps;
	}

	/**
	 * Manually decide whether to filter out system applications.
	 *
	 * Expects the caller to also call notifyDataSetChanged(), if
	 * necessary.
	 */
	public void setFilterSystemApps(boolean newState) {
		if (newState != mHideSystemApps) {
			mHideSystemApps = newState;
			rebuildGroupDisplay();
		}
	}

	public boolean getFilterSystemApps() {
		return mHideSystemApps;
	}

	public void setShowChangedOnly(boolean newState) {
		if (newState != mShowChangedOnly) {
			mShowChangedOnly = newState;
			rebuildGroupDisplay();
		}
	}

	public boolean getShowChangedOnly() {
		return mShowChangedOnly;
	}

	public void setFilterUnknown(boolean newState) {
		if (newState != mHideUnknownEvents) {
			mHideUnknownEvents = newState;
			rebuildGroupDisplay();
		}
	}

	public boolean getFilterUnknown() {
		return mHideUnknownEvents;
	}

	public void setTextFilter(String query) {
		if (query != mTextFilter) {
			mTextFilter = query;
			rebuildGroupDisplay();
		}
	}

	public String getTextFilter() {
		return mTextFilter;
	}


	@SuppressWarnings("serial")
	static class MapOfIntents<K> extends HashMap<K, ArrayList<IntentFilterInfo>> {
		/**
		 * Simplified put() that will automatically create the list
		 * object that is the TreeMap value, and appends to that list.
		 */
		public K put(K key, IntentFilterInfo value) {
			if (!this.containsKey(key)) {
				this.put(key, new ArrayList<IntentFilterInfo>());
			}
			this.get(key).add(value);
			return key;
		}
	}

	/**
	 * Abstract a "group view". We want to allow our data be be shown
	 * in different group modes: group by package, or group by action.
	 *
	 * Rather than using two ExpandableListAdapter implementations
	 * (where we would have to keep the applied filter options etc. in
	 * sync), we instead use a single adapter and abstracting out the
	 * code that is specific to a grouping mode.
	 *
	 */
	static private abstract class GroupingImpl
	{
		public abstract int getGroupCount();
		public abstract Object getGroup(int groupPosition);
		public abstract long getGroupId(int groupPosition);
		public abstract View getGroupView(int groupPosition,
				boolean isExpanded, View convertView, ViewGroup parent);
		public abstract int getChildrenCount(int groupPosition);
		public abstract View getChildView(int groupPosition,
				int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent);
		public abstract long getChildId(int groupPosition, int childPosition);
		public abstract Object getChild(int groupPosition, int childPosition);

		protected MyExpandableListAdapter mParent;

		GroupingImpl(MyExpandableListAdapter parent) {
			mParent = parent;
		}

		/**
		 * Helper for child classes to return a view for a list item.
		 *
		 * If "existing" has a tag that matches "tag", it will be
		 * re-used (this is necessary due to different grouping modes
		 * using different layouts). Otherwise, a new view is created
		 * based on "layout", as a child of "parent".
		 */
		protected View getView(View existing, String tag, int layout,
				ViewGroup parent) {
			if (existing == null || (String)existing.getTag() != "tag")
				return mParent.mInflater.inflate(layout, parent, false);
			else
				return existing;
		}

		/**
		 * Helper for child classes to initialize a "show info" button
		 * that would display information about am event.
		 */
		protected void setActionInfo(View root, final String action) {
			View v = root.findViewById(R.id.show_info);
			if (!Actions.MAP.containsKey(action))
				v.setVisibility(View.GONE);
			else {
				v.setOnClickListener(new OnClickListener() {
					public void onClick(View _v) {
						mParent.mActivity.showInfoToast(action);
					}
				});
			}
		}

		/**
		 * Helper to set the text style for a list item based on
		 * whether it represents a system app, something that disabled
		 * etc.
		 *
		 * Arguments can be null.
		 */
		protected void setTextStyle(TextView t, PackageInfo pkg,
				ComponentInfo comp) {
			if (pkg!= null && pkg.isSystem)
				t.setTextColor(Color.YELLOW);
			else
				t.setTextColor(mParent.mActivity.getResources().getColor(
						android.R.color.primary_text_dark));

			if (comp != null && comp.isCurrentlyEnabled() != comp.defaultEnabled)
				t.setTypeface(Typeface.DEFAULT_BOLD);
			else
				t.setTypeface(Typeface.DEFAULT);
		}

		/**
		 * Helper for child classes to set the text of an item that
		 * represents a component. This adds the component label to "base"
		 * in the rare case one exists, and also makes sure to strike
		 * the text if the component is disabled.
		 */
		protected void setComponentText(TextView t, ComponentInfo comp,
				String base) {
			SpannableStringBuilder fullText = new SpannableStringBuilder();
			fullText.append(base);
			if (comp.componentLabel != null && !comp.componentLabel.equals(""))
				fullText.append(" ("+comp.componentLabel+")");
			if (!comp.isCurrentlyEnabled())
				fullText.setSpan(new StrikethroughSpan(), 0, fullText.length(), 0);
			t.setText(fullText);
		}
	}

	/**
	 * Group by Action.
	 */
	static private class GroupByActionImpl extends GroupingImpl {

		ArrayList<String> mGroups;
		MapOfIntents<String> mChildren;

		GroupByActionImpl(ArrayList<IntentFilterInfo> data,
				MyExpandableListAdapter adapter) {
			super(adapter);

			mGroups = new ArrayList<String>();
			mChildren = new MapOfIntents<String>();

			for (IntentFilterInfo info : data)
			{
				if (adapter.checkAgainstFilters(info))
				{
					if (!mGroups.contains(info.action))
						mGroups.add(info.action);
					mChildren.put(info.action, info);
				}
			}

			// Sort by order of actions in our known action database.
			Collections.sort(mGroups, new Comparator<String>() {
				public int compare(String action1, String action2) {
					return Actions.compare(action1, action2);
				}
			});
			// Sort children by descending priority
			for (ArrayList<IntentFilterInfo> group : mChildren.values()) {
				Collections.sort(group, new Comparator<IntentFilterInfo>() {
					@Override
					public int compare(IntentFilterInfo object1,
							IntentFilterInfo object2) {
						return -Float.compare(object1.priority, object2.priority);
					}
				});
			}
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent)
		{
			String action = (String) getGroup(groupPosition);

			View v = getView(convertView, "act-group",
					R.layout.by_act_group_row, parent);
			setActionInfo(v, action);

			((TextView)v.findViewById(R.id.title)).setText(
					mParent.mActivity.getIntentName(action));

			return v;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent)
		{
			View v = getView(convertView, "act-child",
					R.layout.by_act_child_row, parent);

			IntentFilterInfo info = (IntentFilterInfo) getChild(
					groupPosition, childPosition);
			ComponentInfo comp = info.componentInfo;

			// Set the icon
			ImageView img = ((ImageView)v.findViewById(R.id.icon));
		    img.setImageDrawable(comp.packageInfo.icon);

			// Set the text
			TextView title = ((TextView)v.findViewById(R.id.title));
			setTextStyle(title, comp.packageInfo, comp);
			setComponentText(title, comp, comp.getLabel());

			// Hide the spinner by default
			ProgressBar spinner = (ProgressBar)v.findViewById(R.id.spinner);
			ToggleService toggleService = mParent.mActivity.mToggleService;
			if (toggleService != null && toggleService.has(comp))
				spinner.setVisibility(View.VISIBLE);
			else
				spinner.setVisibility(View.GONE);

			return v;
		}

		public int getGroupCount() {
			return mGroups.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mGroups.get(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return mGroups.get(groupPosition).hashCode();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mChildren.get(mGroups.get(groupPosition)).size();
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return getChild(groupPosition, childPosition).hashCode();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return mChildren.get(mGroups.get(groupPosition)).get(childPosition);
		}
	}

	/**
	 * Group events by package.
	 */
	static private class GroupByPackageImpl extends GroupingImpl {

		ArrayList<PackageInfo> mGroups;
		MapOfIntents<PackageInfo> mChildren;

		GroupByPackageImpl(ArrayList<IntentFilterInfo> data,
				MyExpandableListAdapter adapter) {
			super(adapter);

			mGroups = new ArrayList<PackageInfo>();
			mChildren = new MapOfIntents<PackageInfo>();

			for (IntentFilterInfo info : data)
			{
				if (adapter.checkAgainstFilters(info))
				{
					if (!mGroups.contains(info.componentInfo.packageInfo))
						mGroups.add(info.componentInfo.packageInfo);
					mChildren.put(info.componentInfo.packageInfo, info);
				}
			}

			// Sort groups alphabetically
			Collections.sort(mGroups, new Comparator<PackageInfo>() {
				@Override
				public int compare(PackageInfo object1, PackageInfo object2) {
					return object1.getLabel().compareToIgnoreCase(object2.getLabel());
				}
			});
			// Sort children by our action ordering.
			for (ArrayList<IntentFilterInfo> group : mChildren.values()) {
				Collections.sort(group, new Comparator<IntentFilterInfo>() {
					@Override
					public int compare(IntentFilterInfo object1,
							IntentFilterInfo object2) {
						return Actions.compare(object1.action, object2.action);
					}
				});
			}
		}

		public int getGroupCount() {
			return mGroups.size();
		}

		public long getGroupId(int groupPosition) {
			return mGroups.get(groupPosition).hashCode();
		}

		public Object getGroup(int groupPosition) {
			return mGroups.get(groupPosition);
		}

		public int getChildrenCount(int groupPosition) {
			return mChildren.get(mGroups.get(groupPosition)).size();
		}

		public long getChildId(int groupPosition, int childPosition) {
			return getChild(groupPosition, childPosition).hashCode();
		}

		public Object getChild(int groupPosition, int childPosition) {
			return mChildren.get(mGroups.get(groupPosition)).get(childPosition);
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent)
		{
			PackageInfo pkg = (PackageInfo)getGroup(groupPosition);
			View v = getView(convertView, "pkg-group",
					R.layout.by_pkg_group_row, parent);

			// Set the icon
			ImageView img = ((ImageView)v.findViewById(R.id.icon));
		    img.setImageDrawable(pkg.icon);

		    // Set the text (app name)
			TextView textView = (TextView)v.findViewById(R.id.title);
			textView.setText(pkg.getLabel());
			setTextStyle(textView, pkg, null);

			return v;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent)
		{
			IntentFilterInfo info = (IntentFilterInfo) getChild(
					groupPosition, childPosition);

			View v = getView(convertView, "pkg-child",
					R.layout.by_pkg_child_row, parent);
			setActionInfo(v, info.action);

			TextView text = ((TextView)v.findViewById(R.id.title));
			setTextStyle(text, null, info.componentInfo);
			setComponentText(text, info.componentInfo,
					mParent.mActivity.getIntentName(info.action));

			ImageView infoIcon = ((ImageView)v.findViewById(R.id.show_info));
			ProgressBar spinner = ((ProgressBar)v.findViewById(R.id.spinner));

			ToggleService toggleService = mParent.mActivity.mToggleService;
			if (toggleService != null && toggleService.has(info.componentInfo)) {
				spinner.setVisibility(View.VISIBLE);
				infoIcon.setVisibility(View.GONE);
			}
			else {
				spinner.setVisibility(View.GONE);
				infoIcon.setVisibility(View.VISIBLE);
			}

			return v;
		}
	}

}