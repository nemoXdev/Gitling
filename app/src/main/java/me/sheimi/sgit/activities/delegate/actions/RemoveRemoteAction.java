package me.sheimi.sgit.activities.delegate.actions;

import java.io.IOException;
import java.util.Set;

import me.sheimi.sgit.R;
import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.RemoveRemoteDialog;

public class RemoveRemoteAction extends RepoAction {

    public RemoveRemoteAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        Set<String> remotes = mRepo.getRemotes();
        if (remotes == null || remotes.isEmpty()) {
            mActivity.showToastMessage(R.string.alert_please_add_a_remote);
            return;
        }

        RemoveRemoteDialog dialog = new RemoveRemoteDialog();
        dialog.setArguments(mRepo.getBundle());
        dialog.show(mActivity.getSupportFragmentManager(), "remove-remote-dialog");
        mActivity.closeOperationDrawer();
    }

    public static void removeRemote(Repo repo, RepoDetailActivity activity, String remote) throws IOException {
        repo.removeRemote(remote);
        activity.showToastMessage(R.string.success_remote_removed);
    }

}
