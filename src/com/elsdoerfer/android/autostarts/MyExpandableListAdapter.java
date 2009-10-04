/**
 *
 */
package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.elsdoerfer.android.autostarts.DatabaseHelper.ReceiverData;

/**
 * ListAdapter used by the ListActivity. Has it's own top-level file to
 * keep file sizes small.
 */
public class MyExpandableListAdapter extends BaseExpandableListAdapter {

	private ListActivity mActivity;
	private int mChildLayout;
	private int mGroupLayout;
	private ArrayList<ActionWithReceivers> mDataAll;
	private ArrayList<ActionWithReceivers> mDataFiltered;

	private boolean mShowSystemApps = true;

	private LayoutInflater mInflater;
	private PackageManager mPackageManager;

	public MyExpandableListAdapter(ListActivity activity, int groupLayout, int childLayout) {
		mActivity = activity;
		mChildLayout = childLayout;
		mGroupLayout = groupLayout;
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPackageManager = activity.getPackageManager();
		setData(new ArrayList<ActionWithReceivers>());
	}

	public void setData(ArrayList<ActionWithReceivers> data) {
		mDataAll = data;

		// Create a cached copy of the data containing in a filtered manner.
		// TODO: this should be optimized to support multiple filters, and created on demand?
		// TODO: instead if if()ing on every access, a member field should point to whatever
		// we are currently using.
		mDataFiltered = new ArrayList<ActionWithReceivers>();
		for (ActionWithReceivers row : mDataAll) {
			ActionWithReceivers filtered_row = new ActionWithReceivers(row);
			filtered_row.receivers = new ArrayList<ReceiverData>();  // needs a new (filtered) list
			for (ReceiverData app : row.receivers) {
				if (!ListActivity.isSystemApp(app))
					 filtered_row.receivers.add(app);
			}
			if (filtered_row.receivers.size() > 0)
				mDataFiltered.add(filtered_row);

		}
	}

	public Object getChild(int groupPosition, int childPosition) {
        return getGroupData(groupPosition).receivers.get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return ((ReceiverData)getChild(groupPosition, childPosition)).activityInfo.name.hashCode();
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
		((ImageView)v.findViewById(R.id.icon)).
			setImageDrawable(app.activityInfo.loadIcon(mPackageManager));
		TextView title = ((TextView)v.findViewById(R.id.title));
		if (ListActivity.isSystemApp(app))
		    title.setTextColor(Color.YELLOW);
		else
			title.setTextColor(mActivity.getResources().getColor(android.R.color.primary_text_dark));
		CharSequence appTitle = app.activityInfo.applicationInfo.loadLabel(mPackageManager);
		CharSequence receiverTitle = app.activityInfo.loadLabel(mPackageManager);
		SpannableString fullTitle;
		if (appTitle.equals(receiverTitle))
			fullTitle = SpannableString.valueOf(appTitle);
		else
			fullTitle = SpannableString.valueOf((appTitle + " ("+receiverTitle+")"));
		if (!app.enabled)
			fullTitle.setSpan(new StrikethroughSpan(), 0, fullTitle.length(), 0);
		title.setText(fullTitle);
		return v;
    }

    public Object getGroup(int groupPosition) {
        return getGroupData(groupPosition);
    }

    public int getGroupCount() {
        return getData().size();
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
     * Return the dataset currently used.
     */
    private ArrayList<ActionWithReceivers>getData() {
    	if (mShowSystemApps)
    		return mDataAll;
    	else
    		return mDataFiltered;
    }

    /**
     * Return the data record for the given group.
     */
    private ActionWithReceivers getGroupData(int groupPosition) {
    	if (mShowSystemApps)
    		return mDataAll.get(groupPosition);
    	else
    		return mDataFiltered.get(groupPosition);
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
    	mShowSystemApps = !mShowSystemApps;
    	return !mShowSystemApps;
    }

    /**
     * Manually decide whether to filter out system applications.
     *
     * Expects the caller to also call notifyDataSetChanged(), if
     * necessary.
     */
    public void setFilterSystemApps(boolean newState) {
    	mShowSystemApps = !newState;
    }
}