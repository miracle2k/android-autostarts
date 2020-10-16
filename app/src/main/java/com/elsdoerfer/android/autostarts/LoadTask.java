package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import com.elsdoerfer.android.autostarts.ReceiverReader.OnLoadProgressListener;
import com.elsdoerfer.android.autostarts.db.IntentFilterInfo;


// TODO: We could speed this up (probably not too much) by returning
// only the newly found apps during progress report. I don't think
// the list itself cares when notifyDatasetChanged() is called, but
// at least we don't need to re-filter the whole list on every progress
// report, but can only apply the filter to what comes in new.
class LoadTask extends AsyncTask<Object, Object, ArrayList<IntentFilterInfo>> {

	ListActivity mListActivity = null;
	Integer mCurrentProgress = 0;
	long timeStarted, lastUIUpdate;

	public LoadTask(ListActivity initialConnect) {
		attach(initialConnect);
	}

	public void attach(ListActivity activity) {
		mListActivity = activity;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// Note that we have the current progress remembered (there is
		// no getProgress() apparently), and we need it so that when a
		// new Activity connects after an orientation change, we can
		// display the current progress right away, rather than it taking
		// the time until the next publishProgress() call before we update
		// the progress bar.
		mListActivity.setProgress(mCurrentProgress);
		mListActivity.setProgressBarVisibility(true);
		if (mListActivity.mReloadItem != null)
			mListActivity.mReloadItem.setEnabled(false);
		mListActivity.updateEmptyText();
		timeStarted = lastUIUpdate = SystemClock.elapsedRealtime();
	}

	@Override
	protected ArrayList<IntentFilterInfo> doInBackground(Object... params) {
		ReceiverReader reader = new ReceiverReader(mListActivity, new OnLoadProgressListener() {
			@Override
			public void onProgress(ArrayList<IntentFilterInfo> currentState, float progress) {
				publishProgress(currentState, progress);
			}
		});
		return reader.load();
	}

	@Override
	protected void onPostExecute(ArrayList<IntentFilterInfo> result) {
		// This should not happen, but I have seen it happen.
		// TODO: Get rid of this activity-bound task entirely, try to find a more
		// modern approach.
		if (mListActivity == null)
			return;

		mListActivity.mEvents = result;
		mListActivity.apply();

		mListActivity.setProgressBarVisibility(false);
		if (mListActivity.mReloadItem != null)
			mListActivity.mReloadItem.setEnabled(true);
		mListActivity.updateEmptyText(true);
		Log.d(Utils.TAG, "Loading receivers took " +(SystemClock.elapsedRealtime() - timeStarted));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		if (mListActivity != null) {
			// Updating the UI takes a long time and hugely slows down the UI
			// (to the tune of 30s vs 2s). If loading takes too long, we still
			// want to update once in a while though.
			//
			// Here are my measurements regarding this: (in ms)
			//
			// (not updating the list at all)
			//    6270 2003 1725 1749        (after reinstall)
			//    2952 1917                  (after restart)
			//    2978                       (after reinstall)
			//
			// (updating after every new receiver found)
			//    27395 25893 25828
			//
			// (updating the list every second)
			//    2919 1685

			if (SystemClock.elapsedRealtime() - lastUIUpdate > 2000) {
				mListActivity.mEvents = (ArrayList<IntentFilterInfo>)values[0];
				mListActivity.apply();
				lastUIUpdate = SystemClock.elapsedRealtime();
			}
			mCurrentProgress = (int)(((Float)values[1])*10000);
			mListActivity.setProgress(mCurrentProgress);
		}
	}
}