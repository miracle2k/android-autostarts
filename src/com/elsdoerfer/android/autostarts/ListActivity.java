package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.app.DialogFragment;
import android.app.ExpandableListActivity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.elsdoerfer.android.autostarts.db.ComponentInfo;
import com.elsdoerfer.android.autostarts.db.IntentFilterInfo;

public class ListActivity extends ExpandableListActivity {

	static final Boolean LOGV = false;

	static final String PREFS_NAME = "common";
	static final String PREF_FILTER_SYS_APPS = "filter-sys-apps";
	static final String PREF_FILTER_SHOW_CHANGED = "show-changed-only";
	static final String PREF_FILTER_UNKNOWN = "filter-unknown-events";
	static final String PREF_GROUPING = "grouping";

	private Menu mActionBarMenu;
	private MenuItem mExpandCollapseToggleItem;
	private MenuItem mGroupingModeItem;
	MenuItem mReloadItem;
	private Toast mInfoToast;

	MyExpandableListAdapter mListAdapter;
	ArrayList<IntentFilterInfo> mEvents;
	private DatabaseHelper mDb;
	SharedPreferences mPrefs;
	private Boolean mExpandSuggested = true;
	private LoadTask mLoadTask;

	protected ToggleService mToggleService;

	private ServiceConnection mToggleServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mToggleService = ((ToggleService.LocalBinder)service).getService();
			mToggleService.setHandler(new ToggleService.ToggleServiceListener() {
				@Override
				public void onActivityChange(ComponentInfo component) {
					if (component == null) {
						// Restore quiet state, no item being processed.
						setDefaultTitle();
						ListActivity.this.setProgressBarIndeterminateVisibility(false);
					}
					else {
						// Display info about item being processed
						ListActivity.this.setTitle(String.format(
								getString(R.string.changing_state), component.getLabel()));
						// Note; We are using indeterminate progress mode here, though a
						// proper bar makes sense as well if the user queues multiple changes.
						// It especially makes sense when restoring a backup state.
						// We have to consider a running 'refresh list' process though.
						ListActivity.this.setProgressBarVisibility(false);
						// It seems impossible to show just one progressbar (determinate
						// or indeterminate). The previous line shows the determinate one
						// as well, and setProgressBarVisibility hides the indeterminate one.
						// Setting the progress to 0 sort of works, but on the new flat-blue
						// Android theme anyway you do see the - subtle - empty progressbar.
						ListActivity.this.setProgress(0);
					}
				}

				@Override
				public void onQueueModified(ComponentInfo component, boolean wasAdded) {
					if (!wasAdded) {
						// The component was removed from the queue, presumably
						// successfully processed. We now need to copy the new
						// state from "component" to our local dataset. This is
						// because the "component" instance the service has is a
						// copy of the one used here in the activity, due to it
						// being serialized in a parcel when given to it via an
						// Intent.
						//
						// It's a bit hacky, but the best alternative I can think
						// of would be moving the complete IntentFilterInfo
						// database into a global scope so the service can work
						// directly with it.
						for (IntentFilterInfo info : mEvents) {
							if (info.componentInfo.equals(component))
								info.componentInfo.currentEnabledState =
										component.currentEnabledState;
						}
					}

					mListAdapter.notifyDataSetInvalidated();
				}
			});

			// The list adapter require access to the ToggleService to show
			// current processing, but we can't delay initialing the list
			// adapter right away in onCreate, or the list view will lose
			// open/collapsed state. I'm not aware that we can make the
			// service binding block. My solution therefore is making
			// access to mToggleService optional in our list adapter impl,
			// and refresh the list once, when the service is finally connected.
			mListAdapter.notifyDataSetChanged();

			// Get us an initial UI update.
			mToggleService.requestUpdate();
		}
		public void onServiceDisconnected(ComponentName className) {}
	};


	@Override
	public void onCreate(final Bundle saved) {
		super.onCreate(saved);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);
		setContentView(R.layout.list);
		setDefaultTitle();

		// Set everything up.
		mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		mDb = new DatabaseHelper(this);
		// This is just to workaround "Can't upgrade read-only database..."
		// exceptions, when an upgrade is necessary.
		mDb.getWritableDatabase().close();
		mListAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mListAdapter);

		// Restore preferences
		mListAdapter.setFilterSystemApps(
				mPrefs.getBoolean(PREF_FILTER_SYS_APPS, false));
		mListAdapter.setShowChangedOnly(
				mPrefs.getBoolean(PREF_FILTER_SHOW_CHANGED, false));
		mListAdapter.setFilterUnknown(
				mPrefs.getBoolean(PREF_FILTER_UNKNOWN, true));
		mListAdapter.setGrouping(mPrefs.getString(PREF_GROUPING, "action").equals("package")
				? MyExpandableListAdapter.GROUP_BY_PACKAGE
				: MyExpandableListAdapter.GROUP_BY_ACTION);

		bindService(new Intent(this, ToggleService.class),
				mToggleServiceConnection, Context.BIND_AUTO_CREATE);

		// Init/restore retained and instance data. If we have data
		// retained, we can speed things up significantly by not having
		// to do a load.
		Object retained = getLastNonConfigurationInstance();
		if (retained != null) {
			// Be careful not to copy any objects that reference the
			// activity itself, or we would leak it!
			ListActivity oldActivity = (ListActivity) retained;
			mEvents = oldActivity.mEvents;
			mLoadTask = oldActivity.mLoadTask;
			// Display what we have immediately.
			if (mEvents != null)
				apply();
			// Continue loading in case we're not done yet.
			if (mLoadTask != null)
				mLoadTask.attach(this);
		}
		// Otherwise, we are going to have to init certain data
		// ourselves, and load some from from instance state, if
		// available.
		else {
			if (saved != null) {
				/* here's the place to load values from onSaveInstanceState. We
				 * currently no longer do so, but I'd like to keep the logic structure. */
			}
			else { /* here's the place to load some prefs, if necessary */ }

			// Initial load.
			loadAndApply();
		}

		// This depends both on preferences on loading status.
		updateEmptyText();
	}

	private void setDefaultTitle() {
		setTitle(R.string.app_name);
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
		// Storing the last selected event (needed for dialog persistence)
		// is problematic because saving and loading it as a parcable will
		// break the identify of the ComponentInfo/PackageInfo relations.
		// In other words, the attached ComponentInfo will not be the same
		// object as the one we use to render the list items, so changing
		// the "disabledState" of a mLastSelectedEvent restored in this way
		// will not affect the GUI display. There are different approaches
		// can take to solve this (TODO):
		//   - A complicated scheme where IntentFilterInfo stores the
		//     package and component names as strings, and upon restore
		//     from a bundle will look up the actual ComponentInfo parent
		//     as soon as it becomes available. A kind of delayed-loaded
		//     weak reference.
		//   - Since we're only talking about a single property that is
		//     modified by use (disabled state), this data could be stored
		//     by the Activity as a kind of "overlay", as a list of changes
		//     made by the user. We then would no longer need the
		//     ComponentInfo/PackageInfo relations to have a shared identity.
		//   - The ToggleTool, rather than modifing the ComponentInfo
		//     object, could have the Activity's callback method deal with
		//     the state change. If the component is already loaded, it's
		//     state is changed, otherwise, we can assume it will be loaded
		//     correctly (we can assume? would there be race conditions)?
		//   - We could simply store the list of events globally, bypassing
		//     all these problems.
		// Actually, now with the service, where we do parcel the Component
		// anyway, and update via a callback, this might be a moot problem.
		// We can and should get rid of this TODO.

		// TODO: Note that we do not store the event list. In cases where
		// onRetainNonConfigurationInstance() is not available, we will
		// need to reload all events. It would not be *that* hard to store
		// the events list (of course, this only works if loading is
		// finished): We'd just store a list of PackageInfo parcelables,
		// with all ComponentInfo and IntentFilterInfo children, and upon
		// load would simply have to connect each child with it's parent
		// again. However, in the hope that the new Loader API will allow
		// us to avoid all these problems, we don't bother to implement
		// this now.
		// In case the Loader API should prove to be insufficient, I think
		// we would want to switch to a global state.
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
		if (mDb != null)
			mDb.close();
		// Unattach the activity from a potentially running task.
		if (mLoadTask != null)
			mLoadTask.attach(null);
		if (mToggleService != null) {
			unbindService(mToggleServiceConnection);
			mToggleService = null;
		}
		super.onDestroy();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actionbar, menu);
		mActionBarMenu = menu;

		mExpandCollapseToggleItem = menu.findItem(R.id.expand);
		mGroupingModeItem = menu.findItem(R.id.grouping);
		mReloadItem = menu.findItem(R.id.reload);

		SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
		search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			@Override
			public boolean onQueryTextChange(String query) {
				mListAdapter.setTextFilter(query);
				updateEmptyText();
				mListAdapter.notifyDataSetChanged();
				return true;

			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Proper title for the grouping mode toggle item.
		if (mListAdapter.getGrouping() == MyExpandableListAdapter.GROUP_BY_ACTION) {
			mGroupingModeItem.setTitle(R.string.group_by_package);
			mGroupingModeItem.setIcon(R.drawable.ic_action_action_view_list);
		} else {
			mGroupingModeItem.setTitle(R.string.group_by_action);
			mGroupingModeItem.setIcon(R.drawable.ic_action_action_view_column);
		}

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
			mExpandCollapseToggleItem.setIcon(R.drawable.ic_action_navigation_expand_less);
			mExpandSuggested = false;
		}
		else {
			mExpandCollapseToggleItem.setTitle(R.string.expand_all);
			mExpandCollapseToggleItem.setIcon(R.drawable.ic_action_navigation_expand_more);
			mExpandSuggested = true;
		}

		// Reload button disabled while reloading
		mReloadItem.setEnabled(mLoadTask == null || mLoadTask.getStatus() != AsyncTask.Status.RUNNING);

		// View/Filter Submenu
		menu.findItem(R.id.view_changed_only).setChecked(mListAdapter.getShowChangedOnly());
		menu.findItem(R.id.view_hide_sys_apps).setChecked(mListAdapter.getFilterSystemApps());
		menu.findItem(R.id.view_hide_unknown).setChecked(mListAdapter.getFilterUnknown());

		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.grouping:
			String groupingPref = "";
			if (mListAdapter.getGrouping() == MyExpandableListAdapter.GROUP_BY_ACTION) {
				mListAdapter.setGrouping(MyExpandableListAdapter.GROUP_BY_PACKAGE);
				groupingPref = "package";
			}
			else {
				mListAdapter.setGrouping(MyExpandableListAdapter.GROUP_BY_ACTION);
				groupingPref = "action";
			}
			mListAdapter.notifyDataSetChanged();
			mPrefs.edit().putString(PREF_GROUPING, groupingPref).commit();
			invalidateOptionsMenu();
			return true;

		case R.id.view_hide_sys_apps:
			item.setChecked(!item.isChecked());
			mListAdapter.setFilterSystemApps(item.isChecked());
			mListAdapter.notifyDataSetChanged();
			updateEmptyText();
			mPrefs.edit().putBoolean(ListActivity.PREF_FILTER_SYS_APPS, item.isChecked()).commit();
			return true;

		case R.id.view_changed_only:
			item.setChecked(!item.isChecked());
			mListAdapter.setShowChangedOnly(item.isChecked());
			mListAdapter.notifyDataSetChanged();
			updateEmptyText();
			mPrefs.edit().putBoolean(ListActivity.PREF_FILTER_SHOW_CHANGED, item.isChecked()).commit();
			return true;

		case R.id.view_hide_unknown:
			item.setChecked(!item.isChecked());
			mListAdapter.setFilterUnknown(item.isChecked());
			mListAdapter.notifyDataSetChanged();
			updateEmptyText();
			mPrefs.edit().putBoolean(ListActivity.PREF_FILTER_UNKNOWN, item.isChecked()).commit();
			return true;

		case R.id.expand:
			ExpandableListView lv = getExpandableListView();
			for (int i=mListAdapter.getGroupCount()-1; i>=0; i--)
				if (mExpandSuggested)
					lv.expandGroup(i);
				else
					lv.collapseGroup(i);
			invalidateOptionsMenu();
			return true;

		case R.id.reload:
			loadAndApply();
			return true;

		case R.id.help:
			startActivity(new Intent(this, HelpActivity.class));
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		showEventDetails((IntentFilterInfo) mListAdapter.getChild(groupPosition, childPosition));
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	void apply() {
		mListAdapter.setData(mEvents);
		mListAdapter.notifyDataSetChanged();
	}

	private void loadAndApply() {
		// Never start two tasks at once.
		if (mLoadTask != null && mLoadTask.getStatus() == AsyncTask.Status.RUNNING)
			return;

		mLoadTask = new LoadTask(this);
		mLoadTask.execute();
		invalidateOptionsMenu();
	}

	/**
	 * The "loadIsFinished" argument is needed for the call from LoadTask,
	 * because when it's postExecute() handler is run the task status
	 * has not yet been switched to FINISHED (it's still RUNNING).
	 */
	protected void updateEmptyText(boolean loadIsFinished) {
		// Once loading is done, the empty view switches from a progress bar to a text.
		TextView emptyText = (TextView) findViewById(R.id.empty_text);
		if (mLoadTask != null && mLoadTask.getStatus() == AsyncTask.Status.RUNNING && !loadIsFinished) {
			getExpandableListView().setEmptyView(findViewById((android.R.id.empty)));
		}
		else {
			getExpandableListView().setEmptyView(emptyText);
		}

		if (!mListAdapter.isFiltered())
			emptyText.setText(R.string.no_receivers);
		else if (!mListAdapter.getTextFilter().equals("")) {
			emptyText.setText(R.string.no_search_match);
		}
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
						mActionBarMenu.performIdentifierAction(R.id.view, 0);
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

	protected void updateEmptyText() {
		updateEmptyText(false);
	}

	protected void showEventDetails(IntentFilterInfo event) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("details");
		if (prev != null)
			ft.remove(prev);
		ft.addToBackStack(null);

		DialogFragment newFragment = EventDetailsFragment.newInstance(event);
		newFragment.show(ft, "details");
	}

	protected void addJob(ComponentInfo component, boolean newState) {
		Intent intent = new Intent(this, ToggleService.class);
		intent.putExtra("component", component);
		intent.putExtra("state", newState);
		startService(intent);
	}

	// TODO: Instead of showing a toast, fade in a custom info bar, then
	// fade out. This would be an improvement because we could control
	// it better: Show it longer, but have it disappear when the user
	// clicks on it (toasts don't receive clicks).
	// Consider: https://github.com/johnkil/Android-AppMsg
	public void showInfoToast(String action) {
		Object[] data = Actions.MAP.get(action);
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
		if (data == null) {
			message.setVisibility(View.GONE);
		}
		else {
			if (data[2] != null)  // Hide info text both for null and empty string values.
				info = getResources().getText((Integer)data[2]);
			if (!info.equals("")) {
				message.setText(info);
				message.setVisibility(View.VISIBLE);
			} else {
				message.setVisibility(View.GONE);
			}
		}
		mInfoToast.show();
	}

	/**
	 * Return a name for the given intent; tries the pretty name,
	 * if available, and falls back to the raw class name.
	 */
	String getIntentName(String action) {
		Object[] data = Actions.MAP.get(action);
		if (data == null)
			return action;
		return (data[1] != null) ?
				getResources().getString((Integer)data[1]) :
				(String)data[0];
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