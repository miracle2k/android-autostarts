package com.elsdoerfer.android.autostarts;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.elsdoerfer.android.autostarts.db.ComponentInfo;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Processes a queue of "change component state" requests.
 *
 * This allows the user to make multiple changes without
 * waiting for the previous one to complete.
 *
 * This is not using an IntentService, because we need both
 * information about the progress, as well as information
 * about which components are currently queued (since this
 * all needs to be reflected in the UI).
 */
public class ToggleService extends Service {

	static interface ToggleServiceListener {
		/**
		 * Called whenever the component that the service is currently
		 * processing changes. If the service is done processing the
		 * whole queue, this will be called with null.
		 */
		void onActivityChange(ComponentInfo component);

		/**
		 * Called whenever this service's queue changes (new component added,
		 * or finished processing the current one).
		 */
		void onQueueModified(ComponentInfo component, boolean isAdded);
	}

	/**
	 * The job queue this service processes. We use to separate data
	 * structures for this. The queue holds the order. The target states
	 * (enabled/disabled) are stored in a dictionary. So if the user
	 * changes his request while an item is waiting in the queue, we
	 * can change it directly, instead of processing it twice. In addition,
	 * we need this so we can read the list of components that we are
	 * processing, and render them in the UI accordingly.
	 */
	protected HashMap<ComponentInfo, Boolean> mStates;
	protected LinkedBlockingQueue<ComponentInfo> mQueue;

	private ToggleServiceListener mListener;
	// Because Service has no runOnUIThread or equivalent.
	private Handler mHandler;

	private ComponentInfo mItemBeingProcessed = null;
	private boolean mItemBeingProcessedDesiredState;

	@Override
	public void onCreate() {
		super.onCreate();
		mStates = new LinkedHashMap<ComponentInfo, Boolean>();
		mQueue = new LinkedBlockingQueue<ComponentInfo>();
		mHandler = new Handler();
	}

	// To support API Levels <= 4 (pre 2.0).
	@Override
	public void onStart(Intent intent, int startId) {
		handleStart(intent, startId);
	}

	/**
	 * Clients should start this service with an intent to submit jobs.
	 */
	@Override
	synchronized public int onStartCommand(Intent intent, int flags, int startId) {
		handleStart(intent, startId);

		// TODO: While we would want the service to be sticky, this makes no sense so
		//   long we don't actually persist our change queue somewhere. So we should
		//   do that, and then change the mode here.
		return START_REDELIVER_INTENT;
	}

	void handleStart(Intent intent, int startId) {
		// Add the new job to the queue, or change the desired state
		// of the job currently in the queue.
		ComponentInfo component = intent.getParcelableExtra("component");
		boolean newState = intent.getBooleanExtra("state", false);
		if (mStates.put(component,  newState) == null);
		mQueue.offer(component);

		Log.d(Utils.TAG, "Added "+component+" to service queue, now size: "+mStates.size());
		onQueueModified(component, false);

		// Make sure the queue is running (this should do nothing if it already is).
		processNextItem();
	}

	/**
	 * Process the next item in the queue.
	 *
	 * Shutdown the thread when done.
	 */
	synchronized void processNextItem() {
		// Allow only one item to be processed at a time.
		// TODO: Might it not be sensible/beneficial to do multiple changes at a time?
		if (mItemBeingProcessed != null)
			return;

		// Use queue to determine next item to process
		final ComponentInfo component = mQueue.poll();
		if (component == null) {
			onActivityChange(null);
			Log.d(Utils.TAG, "ToggleService mQueue empty, shutting down");
			stopSelf();
			Log.d(Utils.TAG, "Closing all root shells");
			try {
				RootTools.closeAllShells();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		// See whether we should enable or disable
		final boolean desiredState = mStates.remove(component);

		Log.d(Utils.TAG, "Processing "+component+", remaining items in queue: "+mStates.size());
		onActivityChange(component);

		new Thread(new Runnable() {
			public void run() {
				mItemBeingProcessed = component;
				mItemBeingProcessedDesiredState = desiredState;
				final boolean success =
						ToggleTool.toggleState(ToggleService.this, component, desiredState);

				mItemBeingProcessed = null;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (!success) {
							// Now that we do state toggling in the background in the
							// service, error handling poses new challenges. For now,
							// just show a toast. However, I never liked toasts, of course.
							// Options include notifications, as well as additional clever
							// integration with an activity that might be in the foreground.
							Resources res = getResources();
							Toast toast = Toast.makeText(
								ToggleService.this,
								String.format(res.getString(R.string.state_change_failed),
									// TODO: Instead of the component name, it would be
									// better to refer to the event the user wanted to
									// to remove the component from. Unfortunately, we do
									// not even carry this information along.
									component.getLabel(), component.componentName),
								5000);
							toast.show();
						}

						Log.d(Utils.TAG, "Processing "+component+" done");
						onQueueModified(component, false);
						processNextItem();
					}
				});
			}
		}).start();
	}

	// API for a client to use once it has bound to a service instance.

	public void setHandler(ToggleServiceListener handler) {
		mListener = handler;
	}

	/**
	 * True if the component is being processed by this service, or waiting to be.
	 */
	public boolean has(ComponentInfo component) {
		return (component.equals(mItemBeingProcessed) || mStates.containsKey(component));
	}

	/**
	 * If the given component is in the queue, returns the state (True for enabled,
	 * False for disabled) that the component should be switched to.
	 *
	 * Returns the default value if the component is not in the queue.
	 */
	public boolean getQueuedState(ComponentInfo component, boolean defaultValue) {
		if (component.equals(mItemBeingProcessed))
			return mItemBeingProcessedDesiredState;

		Boolean desiredState = mStates.get(component);
		if (desiredState == null)
			return defaultValue;

		return desiredState;
	}

	/**
	 * Once bound, the client can request that we trigger our events once to send
	 * it an initial status update.
	 */
	public void requestUpdate() {
		onActivityChange(mItemBeingProcessed);
	}

	// Helpers to trigger callbacks.

	private void onActivityChange(ComponentInfo component) {
		if (mListener != null)
			mListener.onActivityChange(component);
	}

	private void onQueueModified(ComponentInfo component, boolean wasAdded) {
		if (mListener != null)
			mListener.onQueueModified(component, wasAdded);
	}

	// The boilerplate to allow access to the service instance from outside

	public class LocalBinder extends Binder {
		ToggleService getService() {
			return ToggleService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
