package com.elsdoerfer.android.autostarts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.elsdoerfer.android.autostarts.DatabaseHelper.ReceiverData;

public class ListActivity extends ExpandableListActivity {

	static final String TAG = "Autostarts";

	static final String KEY_LAST_SELECTED_ITEM = "last-selected-item";

	static final private int MENU_FILTER = 1;
	static final private int MENU_EXPAND_COLLAPSE = 2;
	static final private int MENU_RELOAD = 3;
	static final private int MENU_HELP = 4;

	static final private int DIALOG_RECEIVER_DETAIL = 1;
	static final private int DIALOG_CONFIRM_SYSAPP_CHANGE = 2;

	static final private String PREFS_NAME = "common";
	static final private String PREF_FILTER_SYS_APPS = "filter-sys-apps";


	private MenuItem mExpandCollapseToggleItem;
	private Toast mInfoToast;

	MyExpandableListAdapter mListAdapter;
	private final LinkedHashMap<String, Object[]>
		actionMap = new LinkedHashMap<String, Object[]>();
	private DatabaseHelper mDb;
	private SharedPreferences mPrefs;
	private int[] mLastSelectedItem = { -1, -1 };
	private Boolean mExpandSuggested = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mDb = new DatabaseHelper(this);
        // This is just to workaround "Can't upgrade read-only database..."
        // exceptions, when an upgrade is necessary.
        mDb.getWritableDatabase().close();

        // Convert the list of available actions (and their data) into a
        // ordered hash map which we are than able to easily query by action
        // name.
        for (Object[] action : Actions.ALL)
        	actionMap.put((String)action[0], action);

        Object retained = getLastNonConfigurationInstance();
        if (retained != null)
        	mLastSelectedItem = (int[]) retained;
        else if (savedInstanceState != null) {
        	mLastSelectedItem = savedInstanceState.getIntArray(
        			KEY_LAST_SELECTED_ITEM);
        }

        mListAdapter = new MyExpandableListAdapter(
        		this, R.layout.group_row, R.layout.child_row);
        setListAdapter(mListAdapter);

        // Restore preferences
    	mListAdapter.setFilterSystemApps(
    			mPrefs.getBoolean(PREF_FILTER_SYS_APPS, false));

        // Initial load
        load();
    }

    @Override
	public Object onRetainNonConfigurationInstance() {
    	// XXX: Actually, retain the list of loaded receivers here,
    	// that would actually be worth it.
		return mLastSelectedItem;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putIntArray(KEY_LAST_SELECTED_ITEM, mLastSelectedItem);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mInfoToast != null)
			mInfoToast.cancel();
	}

	@Override
	protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_FILTER, 0, R.string.toggle_sys_apps).
			setIcon(R.drawable.ic_menu_view);
		mExpandCollapseToggleItem =
			menu.add(0, MENU_EXPAND_COLLAPSE, 0, R.string.expand_all).
				setIcon(R.drawable.ic_collapse_expand);
		menu.add(0, MENU_RELOAD, 0, R.string.reload).
			setIcon(R.drawable.ic_menu_refresh);
		menu.add(0, MENU_HELP, 0, R.string.help).
			setIcon(R.drawable.ic_menu_help);
		return true;
	}

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Decide whether we want to offer the option to collapse, or
		// expand, depending on the current group expansion count.
    	ExpandableListView lv = getExpandableListView();
    	int expandCount = 0;
    	int numGroups = mListAdapter.getGroupCount();
		for (int i=numGroups-1; i>=0; i--) {
			if (lv.isGroupExpanded(i))
				expandCount += 1;
		}
		if (expandCount / (float)numGroups >= 0.5) {
			mExpandCollapseToggleItem.setTitle(R.string.collapse_all);
			mExpandSuggested = false;
		}
		else {
			mExpandCollapseToggleItem.setTitle(R.string.expand_all);
			mExpandSuggested = true;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_FILTER:
			boolean newDoFilter;
			if (mListAdapter.toggleFilterSystemApps()) {
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers_filtered);
				newDoFilter = true;
			}
			else {
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers);
				newDoFilter = false;
			}
			mListAdapter.notifyDataSetChanged();
			mPrefs.edit().putBoolean(PREF_FILTER_SYS_APPS, newDoFilter).commit();
		    return true;
		case MENU_EXPAND_COLLAPSE:
			ExpandableListView lv = getExpandableListView();
			for (int i=mListAdapter.getGroupCount()-1; i>=0; i--)
				if (mExpandSuggested)
					lv.expandGroup(i);
				else
					lv.collapseGroup(i);
			return true;
		case MENU_RELOAD:
			load();
			return true;
		case MENU_HELP:
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_RECEIVER_DETAIL)
		{
			final ReceiverData app = (ReceiverData) mListAdapter.getChild(
					mLastSelectedItem[0], mLastSelectedItem[1]);  // Only for the first init through onCreate()
			View v = getLayoutInflater().inflate(
				R.layout.receiver_info_panel, null, false);

			Dialog d = new AlertDialog.Builder(this).setItems(
				new CharSequence[] {
						getResources().getString((app.enabled)
								? R.string.disable
								: R.string.enable),
						getResources().getString(R.string.appliation_info),
						getResources().getString(R.string.find_in_market)},
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which) {
						final ReceiverData app = (ReceiverData) mListAdapter.getChild(
								mLastSelectedItem[0], mLastSelectedItem[1]);
						final Boolean doEnable = !app.enabled;
						switch (which) {
						case 0:
							if (isSystemApp(app) && !doEnable) {
								new AlertDialog.Builder(ListActivity.this)
									.setTitle(R.string.warning)
									.setMessage(R.string.confirm_sys_disable)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											new ToggleTask(ListActivity.this).execute(app, doEnable);
										}
									})
									.setNegativeButton(android.R.string.cancel, null)
									.create()
									.show();
							}
							else
								new ToggleTask(ListActivity.this).execute(app, doEnable);

							break;
						case 1:
							Intent infoIntent = new Intent();
							// From android-cookbook/GroupHome - it notes:
							// "we shouldnt rely on this entrance into the settings app"
							infoIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
							infoIntent.putExtra("com.android.settings.ApplicationPkgName",
			                	app.activityInfo.applicationInfo.packageName);
			                startActivity(infoIntent);
							break;
						case 2:
							try {
								Intent marketIntent = new Intent(Intent.ACTION_VIEW);
								marketIntent.setData(Uri.parse("market://search?q=pname:"+
										app.activityInfo.applicationInfo.packageName));
								startActivity(marketIntent);
							}
							catch (ActivityNotFoundException e) {}
							break;
						}
						dialog.dismiss();
					}
				})
				// Setting a dummy title is necessary for the title
				// control to be activated in the first place,
				// apparently. We assign the actual title in
				// onPrepareDialog().
				.setTitle("dummy").setView(v).create();

			// Due to a bug in Android, onPrepareDialog() is not called
			// when an existing dialog is restored on orientation change.
			// We therefore need to make sure ourselfs that the dialog
			// is initialized correctly in this case as well. Note that
			// our current implementation means that the prepare code
			// will be run twice when the dialog is created for the first
			// time under normal circumstances.
			prepareReceiverDetailDialog(d, v, false);
			return d;
		}
		else
			return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		prepareReceiverDetailDialog(dialog, dialog.getWindow().getDecorView(),
				true);
	}

	/**
	 * We cannot just rename this to "onPrepareDialog()", since the
	 * dialog isn't fully created while processing "onCreateDialog()".
	 *
	 * As a result, the view objects needs to be accessed differently,
	 * and is thus handled via an argument here.
	 */
	private void prepareReceiverDetailDialog(Dialog dialog, View view,
			boolean rewriteCaption) {
		ActionWithReceivers action =
			(ActionWithReceivers) mListAdapter.getGroup(mLastSelectedItem[0]);
		ReceiverData app = (ReceiverData) mListAdapter.getChild(
			mLastSelectedItem[0], mLastSelectedItem[1]);

		dialog.setTitle(app.activityInfo.loadLabel(getPackageManager()));

		// This is a terribly terrible hack to change the menu item caption
		// to match the current state of the selected item. Unfortunately,
		// there doesn't seem to be a better way to change the list items
		// during onPrepare(), except possibly using a dedicated list
		// adapter, which would also be a lot of work.
		//
		// To make things worse, this code results in a strange
		// "requestFeature() must be called before adding content" error
		// when it is called from onCreate(), i.e. before the dialog was
		// shown at least once. As a result, we need to use the
		// "rewriteCaption" caption flag to disable this part in the
		// onCreate() case. Ergo, the caption is initialized correctly in
		// onCreate() by itself, leading to some code duplication.
		//
		// Note that trying to findById(android.R.id.text1) might also work
		// (not tried - all list items have that id, but we need only the
		// first), but seems less stable; The list items having that id is
		// probably not guaranteed.
		if (rewriteCaption) {
			final String searchFor1 = getResources().getString(R.string.disable);
			final String searchFor2 = getResources().getString(R.string.enable);
			final Stack<View> toCheck = new Stack<View>();
			toCheck.add(dialog.getWindow().getDecorView());
			while (!toCheck.isEmpty()) {
				View current = toCheck.pop();
				if (current instanceof TextView && (
						((TextView)current).getText().equals(searchFor1) ||
						((TextView)current).getText().equals(searchFor2)))
				{
					((TextView)current).setText((app.enabled)
							? R.string.disable
							: R.string.enable);
					break;
				}
				// search children
				else if (current instanceof ViewGroup) {
					ViewGroup vg = (ViewGroup)current;
					for (int i=0; i<vg.getChildCount(); i++)
						toCheck.add(vg.getChildAt(i));
				}
			}
		}

		int t = 0;
		SpannableStringBuilder b = new SpannableStringBuilder();
		b.append("Receiver ");
		t = b.length();
		b.append(app.activityInfo.name);
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(" handles action ");
		t = b.length();
		b.append((String)(action.action));
		b.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), t, b.length(), 0);
		b.append(" with priority ");
		t = b.length();
		b.append(Integer.toString(app.priority));
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(".");

		((TextView)view.findViewById(R.id.message)).setText(b);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		mLastSelectedItem[0] = groupPosition;
		mLastSelectedItem[1] = childPosition;
		showDialog(DIALOG_RECEIVER_DETAIL);
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	/**
     * Load the broadcast receivers installed by applications. Unfortunately, this
     * is a lot more difficult than it sounds.
     *
     * There are multiple approaches one might consider taking, all flawed:
     *
     * 1) Loop through the list of installed packages and collect all receivers.
     *    Unfortunately, Android currently does not allow us to query the intents
     *    a receiver has registered for. See the unimplemented
     *    PackageManager.GET_INTENT_FILTERS, and the following URLs:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/4502827143ea9b20
     *       http://groups.google.com/group/android-developers/browse_thread/thread/ef0e4b390552f2c/
     *
     * 2) Use PackageManager.queryBroadcastReceivers() to find all installed
     *    receivers. In the following thread, hackbod initially suggests that
     *    this could be done using a single call: By using an empty intent filter
     *    with no action, all receivers would be returned. Note however that this
     *    is retracted in a later post:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/3ba4f419f0bec3aa/
     *    Even if that worked, it's still be an open question if we could retrieve
     *    the associated intents, or if we'd again run into the problem from 1).
     *
     *    As a result, using this method we would need a list of builtin, supported
     *    broadcast actions, for each of which we query installed receivers
     *    individually.
     *
     *    Unfortunately, this method has another huge downside: Disabled components
     *    are never returned. As a result, if we want to give the user the option
     *    to toggle receivers on and off, once one has been disabled, we need to
     *    employ various hackery to remember this new status, because a requery
     *    will no longer list the receiver. Considering that applications may be
     *    removed/updated, this is a slightly complex proposition.
     *
     * 3) We could parse the apps inside /data/app manually, potentially based on
     *    the OS Android Code (see for example PackageParser.java). However, this
     *    would be complex to write in any case.
     *
     * For now, we are going with 2).
     */
    // TODO: move this to an ASyncTask
	private void load() {
        final PackageManager pm = getPackageManager();

        ArrayList<ActionWithReceivers> receiversByIntent =
        	new ArrayList<ActionWithReceivers>();

        // List of "pckName/compName" entries. We use this to have a quick
        // list of all components available which we discovered through
        // the query process, against which we can check the custom stored
        // component state later on.
        ArrayList<ReceiverData> knownComponents = new ArrayList<ReceiverData>();

        for (Object[] intent : Actions.ALL) {
            Intent query = new Intent();
            query.setAction((String)(intent[0]));
            Log.d(TAG, "Querying receivers for action: "+(String)(intent[0]));
	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query,
	        		PackageManager.GET_INTENT_FILTERS);

	        if (receivers.size() <= 0)
	        	// Don't bother adding empty groups.
	        	continue;

	        ArrayList<ReceiverData> currentAppList = new ArrayList<ReceiverData>();
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo info = receivers.get(i);
	        	ReceiverData data = new ReceiverData();
	        	Log.d(TAG, "Found receiver: "+info.toString());
	        	if (info.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null?!");
	        		continue;
	        	} else {
	        		data.packageName = info.activityInfo.packageName;
	        		data.componentName = info.activityInfo.name;
	        		data.action = (String)intent[0];
	        		data.priority = info.priority;
	        		data.activityInfo = info.activityInfo;
	        		// If we found it here, we can always assume it is enabled
	        		data.enabled = true;
	        	}

	        	currentAppList.add(data);
	        	knownComponents.add(data);
	        }

	        receiversByIntent.add(
	        		new ActionWithReceivers((String)intent[0], currentAppList));
        }

        // Since we can't query the intent filters of disabled receivers,
        // we cache those ourselves in the database. At this point, we
        // load the cache and merge it with the components found through
        // normal discovery.
        ReceiverData[] cachedComponents = mDb.getCachedComponents();
        if (cachedComponents!=null) for (ReceiverData c : cachedComponents) {
        	if (knownComponents.contains(c)) {
        		// We are apparently able to find this via normal recovery,
        		// we can thus move on and delete it from the cache.
        		Log.d(TAG, "Remembered disabled component found through "+
					"normal discovery, deleting from cache");
        		mDb.uncacheComponent(c);
        	}
        	else {
        		try {
					c.init(pm);
				} catch (NameNotFoundException e) {
					// Apparently, this component no longer exists.
					Log.d(TAG, "Remembered disabled component no longer "+
							"exists, deleting from cache");
					mDb.uncacheComponent(c);
					continue;
				}

				// TODO: Check the real enabled state using getComponentEnabledState?
        		if (c.enabled) {
        			// If the component is enabled, we *must* have found it
        			// already. In fact, the contains() code above should
        			// have caught it already, this should never execute,
        			// which is why we log a big warning.
        			Log.d(TAG, "Remembered disabled component was found "+
        					"to be enabled, but for some reason wasn't "+
        					"matched with a discovered comonent. This "+
        					"shouldn't happen");
        			mDb.uncacheComponent(c);
        		}
        		else {
        			Log.d(TAG, "Added disabled component from cache: "+c);
        			// NOTE: This indexOf() call relies on ActionWithIntents
        			// having a proper equals() implementation.
        			ActionWithReceivers testGroup = new ActionWithReceivers(c.action, null);
        			int idx = receiversByIntent.indexOf(testGroup);
        			if (idx == -1) { // No group entry for this action does exist yet.
        				testGroup.receivers = new ArrayList<ReceiverData>();
        				testGroup.receivers.add(c);
        				receiversByIntent.add(testGroup);
        			}
        			else {
        				receiversByIntent.get(idx).receivers.add(c);
        			}
        		}
        	}
        }

        // Sort both groups and children. This ensures that the components
        // we add from the cache take the same place they had before, when
        // they were discovered.
        Collections.sort(receiversByIntent, new Comparator<ActionWithReceivers>() {
			public int compare(ActionWithReceivers object1,
					ActionWithReceivers object2) {
				int idx1 = Utils.getHashMapIndex(actionMap, object1.action);
				int idx2 = Utils.getHashMapIndex(actionMap, object2.action);
				return ((Integer)idx1).compareTo(idx2);
			}
        });
        for (ActionWithReceivers action : receiversByIntent)
        	Collections.sort(action.receivers);

        mListAdapter.setData(receiversByIntent);
        mListAdapter.notifyDataSetChanged();
    }

    // TODO: Instead of showing a toast, fade in a custom info bar, then fade out.
    // This would be an improvement because we could control it better: Show it longer,
    // but have it disappear when the user clicks on it (toasts don't receive clicks).
    public void showInfoToast(ActionWithReceivers action) {
    	Object[] data = actionMap.get(action.action);
    	if (mInfoToast == null) {
    		LayoutInflater inflater = getLayoutInflater();
    		View layout = inflater.inflate(R.layout.detail_toast,
    				(ViewGroup) findViewById(R.id.root));
    		mInfoToast = new Toast(this);
    		mInfoToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    		mInfoToast.setDuration(Toast.LENGTH_LONG);
    		mInfoToast.setView(layout);
    	}
    	((TextView)mInfoToast.getView().findViewById(R.id.title)).setText(getIntentName(action));
    	TextView message = ((TextView)mInfoToast.getView().findViewById(android.R.id.message));
    	CharSequence info = "";
    	if (data[2] != null)  // Hide info text both for null and empty string values.
    		info = getResources().getText((Integer)data[2]);
    	if (!info.equals("")) {
    		message.setText(info);
    		message.setVisibility(View.VISIBLE);
    	} else {
    		message.setVisibility(View.GONE);
    	}
    	mInfoToast.show();
    }

	/**
     * Return a name for the given intent; tries the pretty name,
     * if available, and falls back to the raw class name.
     */
	String getIntentName(ActionWithReceivers action) {
		Object[] data = actionMap.get(action.action);
		return (data[1] != null) ?
				getResources().getString((Integer)data[1]) :
				(String)data[0];
	}

	/**
	 * True if this app is installed on the system partition.
	 */
	static boolean isSystemApp(ReceiverData app) {
		return ((ApplicationInfo.FLAG_SYSTEM & app.activityInfo.applicationInfo.flags)
					== ApplicationInfo.FLAG_SYSTEM);
	}

	/**
	 * Get the current enabled state of a component.
	 */
	static Boolean isComponentEnabled(PackageManager pm, ReceiverData app) throws NameNotFoundException {
		ComponentName c = new ComponentName(
				app.activityInfo.packageName, app.activityInfo.name);
			int setting = pm.getComponentEnabledSetting(c);
			return
				(setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
					? true
					: (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
						? false
						: (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
							? pm.getReceiverInfo(c, PackageManager.GET_DISABLED_COMPONENTS).enabled
							: null;
	}
}