package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.elsdoerfer.android.autostarts.DatabaseHelper.ReceiverData;

public class ListActivity extends ExpandableListActivity {

	static final String TAG = "Autostarts";
	static final Boolean LOGV = false;

	static final private int MENU_VIEW = 1;
	static final private int MENU_EXPAND_COLLAPSE = 2;
	static final private int MENU_RELOAD = 3;
	static final private int MENU_HELP = 4;

	static final private int DIALOG_RECEIVER_DETAIL = 1;
	static final private int DIALOG_CONFIRM_SYSAPP_CHANGE = 2;
	static final private int DIALOG_UNINSTALL_WARNING = 3;
	static final private int DIALOG_VIEW_OPTIONS = 4;

	static final private String PREFS_NAME = "common";
	static final private String PREF_FILTER_SYS_APPS = "filter-sys-apps";
	static final private String PREF_FILTER_ENABLED_APPS = "filter-enabled-apps";
	static final private String PREF_UNINSTALL_WARNING_SHOWN = "uninstall-warning-shown";


	private MenuItem mExpandCollapseToggleItem;
	private Toast mInfoToast;

	MyExpandableListAdapter mListAdapter;
	private LinkedHashMap<String, Object[]> mActionMap;
	private ArrayList<ActionWithReceivers> mReceiversByIntent;
	private DatabaseHelper mDb;
	private SharedPreferences mPrefs;
	private Boolean mExpandSuggested = true;
	private ToggleTask mToggleTask;

	// Due to Android deficiencies (can't pass data to showDialog()),
	// we need to store that data globally.
	private ReceiverData mLastSelectedReceiver;
	private String mLastSelectedAction;
	private boolean mLastChangeRequestDoEnable;
	private boolean mUninstallWarningShown;

	@Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.list);

        // Set everything up.
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mDb = new DatabaseHelper(this);
        // This is just to workaround "Can't upgrade read-only database..."
        // exceptions, when an upgrade is necessary.
        mDb.getWritableDatabase().close();
        mListAdapter = new MyExpandableListAdapter(
        		this, R.layout.group_row, R.layout.child_row);
        setListAdapter(mListAdapter);

        // Restore preferences
    	mListAdapter.setFilterSystemApps(
    			mPrefs.getBoolean(PREF_FILTER_SYS_APPS, false));
    	mListAdapter.setShowDisabledOnly(
    			mPrefs.getBoolean(PREF_FILTER_ENABLED_APPS, false));
    	updateEmptyText();

    	// Init/restore retained and instance data. If we have data
        // retained, we can speed things up significantly by not having
        // to do a load.
        Object retained = getLastNonConfigurationInstance();
        if (retained != null) {
        	// Be careful not to copy any objects that reference the
        	// activity itself, or we would leak it!
        	ListActivity oldActivity = (ListActivity) retained;
        	mLastSelectedReceiver = oldActivity.mLastSelectedReceiver;
        	mLastSelectedAction = oldActivity.mLastSelectedAction;
        	mLastChangeRequestDoEnable = oldActivity.mLastChangeRequestDoEnable;
        	mActionMap = oldActivity.mActionMap;
        	mReceiversByIntent = oldActivity.mReceiversByIntent;
        	mUninstallWarningShown = oldActivity.mUninstallWarningShown;
        	mToggleTask = oldActivity.mToggleTask;
        	if (mToggleTask != null)
        		mToggleTask.connectToActivity(this);
        	apply();
        }
        // Otherwise, we are going to have to init certain data
        // ourselves, and load some from from instance state, if
        // available.
        else {
        	if (saved != null) {
        		mLastSelectedReceiver = saved.getParcelable("selected-receiver");
            	mLastSelectedAction = saved.getString("selected-action");
            	mLastChangeRequestDoEnable = saved.getBoolean("change-do-enable");
            	mUninstallWarningShown = saved.getBoolean("uninstall-warning-shown");
        	}
        	else {
        		mUninstallWarningShown = mPrefs.getBoolean(
        				PREF_UNINSTALL_WARNING_SHOWN, false);
        	}

            // Convert the list of available actions (and their data) into
            // a ordered hash map which we are than able to easily query by
        	// action name.
        	mActionMap = new LinkedHashMap<String, Object[]>();
            for (Object[] action : Actions.ALL)
            	mActionMap.put((String)action[0], action);

            // Initial load.
            loadAndApply();
        }
    }

	@Override
	public Object onRetainNonConfigurationInstance() {
    	// Retain the full activity; it's just simpler as opposed to
    	// copying a bunch of stuff to an array; but we need to be very
    	// careful here not to leak it when we restore.
		return this;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("change-do-enable", mLastChangeRequestDoEnable);
		outState.putParcelable("selected-receiver", mLastSelectedReceiver);
		// No need to store the whole list of receivers for this action as
		// well; we just store the action name (which is unique), and can
		// thus restore based on it.
		outState.putString("selected-action", mLastSelectedAction);
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
		// Unattach the activity from a potentially running task.
		if (mToggleTask != null)
    		mToggleTask.connectToActivity(null);
		super.onDestroy();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_VIEW, 0, R.string.view_options).
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
		case MENU_VIEW:
			showDialog(DIALOG_VIEW_OPTIONS);
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
			loadAndApply();
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
			View v = getLayoutInflater().inflate(
				R.layout.receiver_info_panel, null, false);

			Dialog d = new AlertDialog.Builder(this).setItems(
				new CharSequence[] {
						getResources().getString((mLastSelectedReceiver.enabled)
								? R.string.disable
								: R.string.enable),
						getResources().getString(R.string.appliation_info),
						getResources().getString(R.string.find_in_market)},
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which) {
						mLastChangeRequestDoEnable = !mLastSelectedReceiver.enabled;
						switch (which) {
						case 0:
							if (isSystemApp(mLastSelectedReceiver) && !mLastChangeRequestDoEnable)
								showDialog(DIALOG_CONFIRM_SYSAPP_CHANGE);
							else {
								mToggleTask = new ToggleTask(ListActivity.this);
								mToggleTask.execute(
									mLastSelectedReceiver, mLastChangeRequestDoEnable);
							}
							break;
						case 1:
							Intent infoIntent = new Intent();
							// From android-cookbook/GroupHome - it notes:
							// "we shouldn't rely on this entrance into the settings app"
							infoIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
							infoIntent.putExtra("com.android.settings.ApplicationPkgName",
			                	mLastSelectedReceiver.activityInfo.applicationInfo.packageName);
			                startActivity(infoIntent);
							break;
						case 2:
							try {
								Intent marketIntent = new Intent(Intent.ACTION_VIEW);
								marketIntent.setData(Uri.parse("market://search?q=pname:"+
										mLastSelectedReceiver.activityInfo.applicationInfo.packageName));
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
			// We therefore need to make sure ourselves that the dialog
			// is initialized correctly in this case as well. Note that
			// our current implementation means that the prepare code
			// will be run twice when the dialog is created for the first
			// time under normal circumstances.
			prepareReceiverDetailDialog(d, v, false);
			return d;
		}

		else if (id == DIALOG_CONFIRM_SYSAPP_CHANGE)
		{
			return new AlertDialog.Builder(ListActivity.this)
				.setTitle(R.string.warning)
				.setMessage(R.string.confirm_sys_disable)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mToggleTask = new ToggleTask(ListActivity.this);
						mToggleTask.execute(
							mLastSelectedReceiver, mLastChangeRequestDoEnable);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
		}

		else if (id == DIALOG_UNINSTALL_WARNING)
		{
			return new AlertDialog.Builder(ListActivity.this)
				.setTitle(R.string.warning)
				.setMessage(R.string.uninstall_warning)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok, null)
				.create();
		}

		else if (id == DIALOG_VIEW_OPTIONS)
		{
			// We are just hoping that the state of these vars can
			// never change without going through this dialog;
			// otherwise, we'd need to find a solution to ensure
			// that when the instance is reused for a showing at a
			// later point, that the state is still up-to-date, i.e.
			// setting the current state in onPrepareDialog. It's not
			// clear if making the state array a class member and
			// updating it would be enough, or if we actually have to
			// find the correct views and switch them...
			boolean[] initState = new boolean[] {
				mListAdapter.getFilterSystemApps(),
				false,
			};

			return new AlertDialog.Builder(this)
				.setMultiChoiceItems(new CharSequence[] {
						getString(R.string.hide_sys_apps),
						getString(R.string.show_disabled_only),
					},
					initState,
					new OnMultiChoiceClickListener() {
						public void onClick(DialogInterface dialog, int which,
								boolean isChecked) {
							if (which == 0) {
								mListAdapter.setFilterSystemApps(isChecked);
								mListAdapter.notifyDataSetChanged();
								updateEmptyText();
								mPrefs.edit().putBoolean(PREF_FILTER_SYS_APPS, isChecked).commit();
							}
							else if (which == 1) {
								mListAdapter.setShowDisabledOnly(isChecked);
								mListAdapter.notifyDataSetChanged();
								updateEmptyText();
								mPrefs.edit().putBoolean(PREF_FILTER_ENABLED_APPS, isChecked).commit();
							}
						}

					})
				.setPositiveButton(android.R.string.ok, null).create();
		}

		else
			return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == DIALOG_RECEIVER_DETAIL) {
			prepareReceiverDetailDialog(dialog, dialog.getWindow().getDecorView(),
					true);
		}
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

		dialog.setTitle(mLastSelectedReceiver.activityInfo.loadLabel(getPackageManager()));

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
					((TextView)current).setText((mLastSelectedReceiver.enabled)
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
		b.append(mLastSelectedReceiver.activityInfo.name);
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(" handles action ");
		t = b.length();
		b.append((String)(mLastSelectedAction));
		b.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), t, b.length(), 0);
		b.append(" with priority ");
		t = b.length();
		b.append(Integer.toString(mLastSelectedReceiver.priority));
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(".");

		((TextView)view.findViewById(R.id.message)).setText(b);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		mLastSelectedAction =
			((ActionWithReceivers) mListAdapter.getGroup(groupPosition)).action;
		mLastSelectedReceiver =
			(ReceiverData) mListAdapter.getChild(groupPosition, childPosition);
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
	private ArrayList<ActionWithReceivers> load() {
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
            if (LOGV) Log.v(TAG, "Querying receivers for action: "+(String)(intent[0]));
	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query,
	        		PackageManager.GET_INTENT_FILTERS);

	        if (receivers.size() <= 0)
	        	// Don't bother adding empty groups.
	        	continue;

	        ArrayList<ReceiverData> currentAppList = new ArrayList<ReceiverData>();
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo info = receivers.get(i);
	        	ReceiverData data = new ReceiverData();
	        	if (LOGV) Log.v(TAG, "Found receiver: "+info.toString());
	        	if (info.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null for "+info.toString()+"?!");
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
					"normal discovery, deleting from cache: "+c);
        		mDb.uncacheComponent(c);
        	}
        	else {
        		try {
					c.init(pm);
				} catch (NameNotFoundException e) {
					// Apparently, this component no longer exists.
					Log.d(TAG, "Remembered disabled component no longer "+
							"exists, deleting from cache: "+c);
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
        					"shouldn't happen: "+c);
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
				int idx1 = Utils.getHashMapIndex(mActionMap, object1.action);
				int idx2 = Utils.getHashMapIndex(mActionMap, object2.action);
				return ((Integer)idx1).compareTo(idx2);
			}
        });
        for (ActionWithReceivers action : receiversByIntent)
        	Collections.sort(action.receivers);

        return receiversByIntent;
    }

	private void apply() {
		mListAdapter.setData(mReceiversByIntent);
        mListAdapter.notifyDataSetChanged();
	}

	private void loadAndApply() {
		mReceiversByIntent = load();
		apply();
	}

	public void showUninstallWarning() {
		if (!mUninstallWarningShown) {
			showDialog(DIALOG_UNINSTALL_WARNING);
			mUninstallWarningShown = true;
			mPrefs.edit().putBoolean(PREF_UNINSTALL_WARNING_SHOWN, true).commit();
		}
	}

	protected void updateEmptyText() {
		TextView emptyText = (TextView) findViewById(android.R.id.empty);
		if (!mListAdapter.isFiltered())
			emptyText.setText(R.string.no_receivers);
		else {
			// Unfortunately, we cannot link directly from the resource
			// string, and neither can we use {@see Linkify}. In both
			// cases, we end up with a URLSpan instance in the text (the
			// class is apparently hardcoded), which only allows link
			// resolving via Intent. We however want to use our own
			// URLSpan subclass which can trigger an event.
			//
			// We cannot override {@see Linkify} either, really - it
			// makes the one method we could override to easily inject
			// our custom subclass "final" - aaarg.
			CharSequence base = getString(R.string.no_receivers_filtered) +
			   "\n\n";
			SpannableString full = new SpannableString(
					base +
					getString(R.string.change_filter_settings)
			);
			full.setSpan(new InternalURLSpan(new OnClickListener() {

				public void onClick(View v) {
					showDialog(DIALOG_VIEW_OPTIONS);

				}

			}), base.length(), full.length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			emptyText.setText(full);

			// Copied from "Linkify.addLinkMovementMethod" (which is private).
	        MovementMethod m = emptyText.getMovementMethod();
	        if ((m == null) || !(m instanceof LinkMovementMethod)) {
	            if (emptyText.getLinksClickable()) {
	            	emptyText.setMovementMethod(LinkMovementMethod.getInstance());
	            }
	        }
		}
	}

    // TODO: Instead of showing a toast, fade in a custom info bar, then fade out.
    // This would be an improvement because we could control it better: Show it longer,
    // but have it disappear when the user clicks on it (toasts don't receive clicks).
    public void showInfoToast(ActionWithReceivers action) {
    	Object[] data = mActionMap.get(action.action);
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
		Object[] data = mActionMap.get(action.action);
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
	 * URLSpan class that supports a custom onClick listener, rather than
	 * sending everything through an Intent.
	 */
	static class InternalURLSpan extends ClickableSpan {
		OnClickListener mListener;

		public InternalURLSpan(OnClickListener listener) {
			mListener = listener;
		}

	    @Override
	    public void onClick(View widget) {
	    	mListener.onClick(widget);
	    }
	}
}