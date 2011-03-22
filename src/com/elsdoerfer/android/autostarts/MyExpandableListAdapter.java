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
import android.widget.TextView;

import com.elsdoerfer.android.autostarts.ComponentInfo.IntentFilterInfo;

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
		if (mHideSystemApps && comp.isSystem)
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
		return mHideSystemApps || mShowChangedOnly || mHideUnknownEvents;
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
	}

	/**
	 * Group by Action.
	 */
	static private class GroupByActionImpl extends GroupingImpl {

		MyExpandableListAdapter mParent;
		ArrayList<String> mGroups;
		MapOfIntents<String> mChildren;

		GroupByActionImpl(ArrayList<IntentFilterInfo> data, MyExpandableListAdapter adapter) {
			mParent = adapter;

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
					int idx1 = Utils.getHashMapIndex(Actions.MAP, action1);
					int idx2 = Utils.getHashMapIndex(Actions.MAP, action2);
					// Make sure that unknown intents (-1) are sorted at the bottom.
					if (idx1 == -1 && idx2 == -1)
						return action1.compareTo(action2);
					else if (idx1 == -1)
						return +1;
					else if (idx2 == -1)
						return -1;
					else
						return ((Integer)idx1).compareTo(idx2);
				}
			});
			// XXX: Sort children, by priority
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			final View v;
			if (convertView == null || (String)convertView.getTag() != "act-group")
				v = mParent.mInflater.inflate(R.layout.by_act_group_row, parent, false);
			else
				v = convertView;
			final String action = (String) getGroup(groupPosition);
			((TextView)v.findViewById(R.id.title)).setText(
					mParent.mActivity.getIntentName(action));
			((View)v.findViewById(R.id.show_info)).setOnClickListener(new OnClickListener() {
				public void onClick(View _v) {
					mParent.mActivity.showInfoToast(action);
				}
			});
			return v;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null || (String)convertView.getTag() != "act-child")
				v = mParent.mInflater.inflate(R.layout.by_act_child_row, parent, false);
			else
				v = convertView;
			IntentFilterInfo info = (IntentFilterInfo) getChild(
					groupPosition, childPosition);
			ComponentInfo comp = info.componentInfo;

			// Set the icon
			ImageView img = ((ImageView)v.findViewById(R.id.icon));
		    img.setImageDrawable(comp.icon);

			// Set the texts style
			TextView title = ((TextView)v.findViewById(R.id.title));
			if (comp.isSystem)
				title.setTextColor(Color.YELLOW);
			else
				title.setTextColor(mParent.mActivity.getResources().getColor(android.R.color.primary_text_dark));
			if (comp.isCurrentlyEnabled() != comp.defaultEnabled)
				title.setTypeface(Typeface.DEFAULT_BOLD);
			else
				title.setTypeface(Typeface.DEFAULT);

			// Build the text itself
			SpannableStringBuilder fullTitle = new SpannableStringBuilder();
			fullTitle.append(comp.getAppLabel());
			if (comp.componentLabel != null && !comp.componentLabel.equals(""))
				fullTitle.append(" ("+comp.componentLabel+")");
			if (!comp.isCurrentlyEnabled())
				fullTitle.setSpan(new StrikethroughSpan(), 0, fullTitle.length(), 0);
			title.setText(fullTitle);
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

		MyExpandableListAdapter mParent;
		ArrayList<String> mGroups;
		MapOfIntents<String> mChildren;

		GroupByPackageImpl(ArrayList<IntentFilterInfo> data, MyExpandableListAdapter adapter) {
			mParent = adapter;

			mGroups = new ArrayList<String>();
			mChildren = new MapOfIntents<String>();

			for (IntentFilterInfo info : data)
			{
				if (adapter.checkAgainstFilters(info))
				{
					if (!mGroups.contains(info.componentInfo.packageName))
						mGroups.add(info.componentInfo.packageName);
					mChildren.put(info.componentInfo.packageName, info);
				}
			}

			Collections.sort(mGroups, new Comparator<String>() {
				@Override
				public int compare(String object1, String object2) {
					return object1.compareToIgnoreCase(object2);
				}
			});
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
				View convertView, ViewGroup parent) {
			final View v;
			if (convertView == null || (String)convertView.getTag() != "pkg-group")
				v = mParent.mInflater.inflate(R.layout.by_pkg_group_row, parent, false);
			else
				v = convertView;

			// Set the icon
			ImageView img = ((ImageView)v.findViewById(R.id.icon));
		    img.setImageDrawable(((IntentFilterInfo) getChild(groupPosition, 0)).componentInfo.icon);

			final String p = ((IntentFilterInfo) getChild(groupPosition, 0)).componentInfo.getAppLabel();
			((TextView)v.findViewById(R.id.title)).setText(p);
			return v;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null || (String)convertView.getTag() != "pkg-child")
				v = mParent.mInflater.inflate(R.layout.by_pkg_child_row, parent, false);
			else
				v = convertView;
			IntentFilterInfo info = (IntentFilterInfo) getChild(
					groupPosition, childPosition);
			// Set the texts style
			TextView title = ((TextView)v.findViewById(R.id.title));
			title.setText(mParent.mActivity.getIntentName(info.action));
			return v;
		}
	}

}