/**
 *
 */
package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.elsdoerfer.android.autostarts.ReceiverReader.ActionWithReceivers;
import com.elsdoerfer.android.autostarts.ReceiverReader.ReceiverData;

/**
 * ListAdapter used by the ListActivity. Has it's own top-level file to
 * keep file sizes small.
 */
public class MyExpandableListAdapter extends BaseExpandableListAdapter {

	private ListActivity mActivity;
	private int mChildLayout;
	private int mGroupLayout;
	private ArrayList<ActionWithReceivers> mDataAll;
	private ArrayList<ActionWithReceivers> mDataRender;

	private boolean mHideSystemApps = false;
	private boolean mHideUnknownEvents = false;
	private boolean mShowChangedOnly = false;

	private LayoutInflater mInflater;

	public MyExpandableListAdapter(ListActivity activity, int groupLayout, int childLayout) {
		mActivity = activity;
		mChildLayout = childLayout;
		mGroupLayout = groupLayout;
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setData(new ArrayList<ActionWithReceivers>());
	}

	public void setData(ArrayList<ActionWithReceivers> data) {
		mDataAll = data;
		// Re-apply our filters.
		updateRenderData();
	}

	/**
	 * Based on the full data available, updates the data set we use to
	 * display the list; i.e. if there are filters set, those are applied.
	 *
	 * TODO: Add a way to init all filters without updating the data
	 * once for every filter option. Simplest way: generate this on demand?
	 */
	private void updateRenderData() {
		// Short-circuit - no filters
		if (!isFiltered())
			mDataRender = mDataAll;

		else {
			mDataRender = new ArrayList<ActionWithReceivers>();
			for (ActionWithReceivers row : mDataAll) {
				ActionWithReceivers filtered_row = new ActionWithReceivers(row);
				filtered_row.receivers = new ArrayList<ReceiverData>();  // needs a new (filtered) list
				for (ReceiverData app : row.receivers) {
					boolean match = true;
					if (mHideSystemApps && app.isSystem)
						match = false;
					if (mShowChangedOnly && app.isCurrentlyEnabled() == app.defaultEnabled)
						match = false;
					if (mHideUnknownEvents && Utils.getHashMapIndex(Actions.MAP, app.action) == -1)
						match = false;

					if (match)
						filtered_row.receivers.add(app);
				}
				if (filtered_row.receivers.size() > 0)
					mDataRender.add(filtered_row);
			}
		}
	}

	public Object getChild(int groupPosition, int childPosition) {
		return getGroupData(groupPosition).receivers.get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return ((ReceiverData)getChild(groupPosition, childPosition)).componentName.hashCode();
	}

	public int getChildrenCount(int groupPosition) {
		return getGroupData(groupPosition).receivers.size();
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {
		View v;
		if (convertView == null)
			v = mInflater.inflate(mChildLayout, parent, false);
		else
			v = convertView;
		ReceiverData app = (ReceiverData) getChild(groupPosition, childPosition);

		// Set the icon
		ImageView img = ((ImageView)v.findViewById(R.id.icon));
		img.setImageDrawable(app.icon);

		// Set the texts style
		TextView title = ((TextView)v.findViewById(R.id.title));
		if (app.isSystem)
			title.setTextColor(Color.YELLOW);
		else
			title.setTextColor(mActivity.getResources().getColor(android.R.color.primary_text_dark));
		if (app.isCurrentlyEnabled() != app.defaultEnabled)
			title.setTypeface(Typeface.DEFAULT_BOLD);
		else
			title.setTypeface(Typeface.DEFAULT);

		// Build the text itself
		SpannableStringBuilder fullTitle = new SpannableStringBuilder();
		fullTitle.append(app.getAppLabel());
		if (app.componentLabel != null && !app.componentLabel.equals(""))
			fullTitle.append(" ("+app.componentLabel+")");
		if (!app.isCurrentlyEnabled())
			fullTitle.setSpan(new StrikethroughSpan(), 0, fullTitle.length(), 0);
		title.setText(fullTitle);
		return v;
	}

	public Object getGroup(int groupPosition) {
		return getGroupData(groupPosition);
	}

	public int getGroupCount() {
		return mDataRender.size();
	}

	public long getGroupId(int groupPosition) {
		return getGroupData(groupPosition).action.hashCode();
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {
		final View v;
		if (convertView == null)
			v = mInflater.inflate(mGroupLayout, parent, false);
		else
			v = convertView;
		final ActionWithReceivers group = (ActionWithReceivers) getGroup(groupPosition);
		((TextView)v.findViewById(R.id.title)).setText(mActivity.getIntentName(group));
		((View)v.findViewById(R.id.show_info)).setOnClickListener(new OnClickListener() {
			public void onClick(View _v) {
				mActivity.showInfoToast(group);
			}
		});
		return v;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public boolean hasStableIds() {
		return true;
	}

	/**
	 * Return the data record for the given group.
	 */
	 private ActionWithReceivers getGroupData(int groupPosition) {
		 return mDataRender.get(groupPosition);
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
			 updateRenderData();
		 }
	 }

	 public boolean getFilterSystemApps() {
		 return mHideSystemApps;
	 }

	 public void setShowChangedOnly(boolean newState) {
		 if (newState != mShowChangedOnly) {
			 mShowChangedOnly = newState;
			 updateRenderData();
		 }
	 }

	 public boolean getShowChangedOnly() {
		 return mShowChangedOnly;
	 }

	 public void setFilterUnknown(boolean newState) {
		 if (newState != mHideUnknownEvents) {
			 mHideUnknownEvents = newState;
			 updateRenderData();
		 }
	 }

	 public boolean getFilterUnknown() {
		 return mHideUnknownEvents;
	 }

}