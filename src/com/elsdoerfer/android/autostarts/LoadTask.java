package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import android.os.AsyncTask;
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
		mListActivity.mEvents = result;
		mListActivity.apply();

		mListActivity.setProgressBarVisibility(false);
		if (mListActivity.mReloadItem != null)
			mListActivity.mReloadItem.setEnabled(true);
		mListActivity.updateEmptyText(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		if (mListActivity != null) {
			mListActivity.mEvents = (ArrayList<IntentFilterInfo>)values[0];
			mListActivity.apply();
			mCurrentProgress = (int)(((Float)values[1])*10000);
			mListActivity.setProgress(mCurrentProgress);
		}
	}
}