package me.sheimi.sgit.repo.tasks;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.StringRes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import me.sheimi.sgit.R;
import timber.log.Timber;

/**
 * Simple async task replacement for Android's deprecated {@code AsyncTask}.
 *
 * Subclasses should implement {@link #doInBackground(Object[])} and may
 * override {@link #onPreExecute()}, {@link #onProgressUpdate(Object[])}, and
 * {@link #onPostExecute(Object)}.
 */
public abstract class SheimiAsyncTask<Params, Progress, Result> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    protected Throwable mException;
    protected int mErrorRes = 0;

    private volatile boolean mIsCanceled = false;
    private Future<?> mFuture;

    protected void setException(Throwable e) {
        Timber.e(e, "set exception");
        mException = e;
    }

    protected void setException(Throwable e, int errorRes) {
        Timber.e(e, "set error [%d] exception", errorRes);
        mException = e;
        mErrorRes = errorRes;
    }

    protected void setError(int errorRes) {
        Timber.e("set error res id: %d", errorRes);
        mErrorRes = errorRes;
    }

    public void cancelTask() {
        mIsCanceled = true;
        if (mFuture != null) {
            mFuture.cancel(true);
        }
    }

    /**
     * This method is to be overridden and should return the resource that
     * is used as the title as the
     * {@link com.manichord.mgit.dialogs.ErrorDialog} title when the
     * task fails with an exception.
     */
    @StringRes
    public int getErrorTitleRes() {
        return R.string.dialog_error_title;
    }

    public boolean isTaskCanceled() {
        return mIsCanceled;
    }

    protected void publishProgress(final Progress... values) {
        MAIN_HANDLER.post(() -> onProgressUpdate(values));
    }

    protected void executeTask(final Params... params) {
        if (mIsCanceled) {
            return;
        }

        onPreExecute();

        mFuture = EXECUTOR.submit(() -> {
            final Result result;
            try {
                result = doInBackground(params);
            } catch (Throwable t) {
                setException(t);
                return;
            }

            MAIN_HANDLER.post(() -> onPostExecute(result));
        });
    }

    protected void onPreExecute() {
        // Optional override
    }

    protected void onProgressUpdate(Progress... values) {
        // Optional override
    }

    protected void onPostExecute(Result result) {
        // Optional override
    }

    protected abstract Result doInBackground(Params... params);

    public static interface AsyncTaskPostCallback {
        void onPostExecute(Boolean isSuccess);
    }

    public static interface AsyncTaskCallback {
        boolean doInBackground(Void... params);

        void onPreExecute();

        void onProgressUpdate(String... progress);

        void onPostExecute(Boolean isSuccess);
    }
}
