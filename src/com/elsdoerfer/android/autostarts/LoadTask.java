package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.os.AsyncTask;

import com.elsdoerfer.android.autostarts.ReceiverReader.ActionWithReceivers;
import com.elsdoerfer.android.autostarts.ReceiverReader.OnLoadProgressListener;


// TODO: We could speed this up (probably not too much) by returning
// only the newly found apps during progress report. I don't think
// the list itself cares when notifyDatasetChanged() is called, but
// at least we don't need to re-filter the whole list on every progress
// report, but can only apply the filter to what comes in new.
class LoadTask extends AsyncTask<Object, ArrayList<ActionWithReceivers>, ArrayList<ActionWithReceivers>> {

	private volatile ListActivity mActivity;
	private boolean mPostProcessingDone;
	private ArrayList<ActionWithReceivers> mResult;

	public LoadTask(ListActivity wrapActivity) {
		super();
		mPostProcessingDone = false;
		connectToActivity(wrapActivity);
	}

	public void connectToActivity(ListActivity wrapActivity) {
		mActivity = wrapActivity;

		if (mActivity != null) {
			// Set the task up with the new activity.
			if (getStatus() == Status.RUNNING)
				onPreExecute();

			// If we were unable to do the full post processing because of
			// no activity being available, do so now.
			else if (getStatus() == Status.FINISHED && !mPostProcessingDone)
				processPostExecute();
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mActivity.setProgressBarIndeterminateVisibility(true);
	}

	@Override
	protected ArrayList<ActionWithReceivers> doInBackground(
			Object... params) {
		ReceiverReader reader = new ReceiverReader(mActivity, new OnLoadProgressListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onProgress(ArrayList<ActionWithReceivers> currentState) {
				publishProgress(currentState);
			}
		});
		return reader.load();
	}

	@Override
	protected void onPostExecute(ArrayList<ActionWithReceivers> result) {
		super.onPostExecute(result);

		mResult = result;

		// We need to make sure we only go on if an activity is
		// attached. Since it's possible that, say, an orientation
		// change happens while we are running, it can happen that
		// there isn't one. If so, processPostExecute() will be
		// run the next time one is attached.
		if (mActivity != null)
			processPostExecute();
	}

	private void processPostExecute() {
		mPostProcessingDone = true;
		mActivity.mReceiversByIntent = mResult;
		mActivity.apply();

		mActivity.setProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onProgressUpdate(
			ArrayList<ActionWithReceivers>... values) {
		super.onProgressUpdate(values);
		if (mActivity != null) {
			mActivity.mReceiversByIntent = values[0];
			mActivity.apply();
		}
	}
}