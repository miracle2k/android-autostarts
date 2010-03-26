package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import com.elsdoerfer.android.autostarts.ReceiverReader.ActionWithReceivers;
import com.elsdoerfer.android.autostarts.ReceiverReader.OnLoadProgressListener;


// TODO: We could speed this up (probably not too much) by returning
// only the newly found apps during progress report. I don't think
// the list itself cares when notifyDatasetChanged() is called, but
// at least we don't need to re-filter the whole list on every progress
// report, but can only apply the filter to what comes in new.
class LoadTask extends ActivityAsyncTask<ListActivity, Object, Object, ArrayList<ActionWithReceivers>> {

	Integer mCurrentProgress = 0;

	public LoadTask(ListActivity initialConnect) {
		super(initialConnect);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mWrapped.setProgressBarIndeterminateVisibility(true);
		// Note that we have the current progress remembered (there is
		// no getProgress() apparently), and we need it so that when a
		// new Activity connects after an orientation change, we can
		// display the current progress right away, rather than it taking
		// the time until the next publishProgress() call before we update
		// the progress bar.
		mWrapped.setProgress(mCurrentProgress);
		mWrapped.setProgressBarVisibility(true);
		if (mWrapped.mReloadItem != null)
			mWrapped.mReloadItem.setEnabled(false);
		mWrapped.updateEmptyText();
	}

	@Override
	protected ArrayList<ActionWithReceivers> doInBackground(
			Object... params) {
		ReceiverReader reader = new ReceiverReader(mWrapped, new OnLoadProgressListener() {
			@Override
			public void onProgress(ArrayList<ActionWithReceivers> currentState, float progress) {
				publishProgress(currentState, progress);
			}
		});
		return reader.load();
	}

	@Override
	protected void processPostExecute(ArrayList<ActionWithReceivers> result) {
		mWrapped.mReceiversByIntent = result;
		mWrapped.apply();

		mWrapped.setProgressBarIndeterminateVisibility(false);
		mWrapped.setProgressBarVisibility(false);
		if (mWrapped.mReloadItem != null)
			mWrapped.mReloadItem.setEnabled(true);
		mWrapped.updateEmptyText(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		if (mWrapped != null) {
			mWrapped.mReceiversByIntent = (ArrayList<ActionWithReceivers>)values[0];
			mWrapped.apply();
			mCurrentProgress = (int)(((Float)values[1])*10000);
			mWrapped.setProgress(mCurrentProgress);
		}
	}
}