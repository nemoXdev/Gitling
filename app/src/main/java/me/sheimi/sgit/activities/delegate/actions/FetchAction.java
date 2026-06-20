package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;

public class FetchAction extends RepoAction {
    public FetchAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        com.manichord.mgit.dialogs.FetchDialog.show(
                mActivity.findViewById(android.R.id.content), mRepo, mActivity);
        mActivity.closeOperationDrawer();
    }
}
