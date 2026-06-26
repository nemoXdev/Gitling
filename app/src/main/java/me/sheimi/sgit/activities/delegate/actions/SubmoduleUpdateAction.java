package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.repo.tasks.repo.SubmoduleUpdateTask;

public class SubmoduleUpdateAction extends RepoAction {

    public SubmoduleUpdateAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        new SubmoduleUpdateTask(mRepo, null).executeTask();
        mActivity.closeOperationDrawer();
    }
}
