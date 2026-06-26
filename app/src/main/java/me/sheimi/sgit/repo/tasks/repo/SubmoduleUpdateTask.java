package me.sheimi.sgit.repo.tasks.repo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;

import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;

public class SubmoduleUpdateTask extends RepoOpTask {

    private final AsyncTaskCallback mCallback;

    public SubmoduleUpdateTask(Repo repo, AsyncTaskCallback callback) {
        super(repo);
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Git git;
        try {
            git = mRepo.getGit();
        } catch (StopTaskException e) {
            return false;
        }

        try {
            new SubmoduleInitCommand(git.getRepository()).call();
            new SubmoduleUpdateCommand(git.getRepository()).call();
        } catch (Exception e) {
            setException(e, R.string.error_submodule_update_failed);
            return false;
        } catch (OutOfMemoryError e) {
            setException(e, R.string.error_out_of_memory);
            return false;
        }

        setSuccessMsg(R.string.toast_submodule_update_success);

        if (mCallback != null) {
            mCallback.doInBackground(params);
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }
}
