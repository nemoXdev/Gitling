package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.MergeDialog;

public class MergeAction extends RepoAction {

    public MergeAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        MergeDialog md = new MergeDialog();
        md.setArguments(mRepo.getBundle());
        md.show(mActivity.getSupportFragmentManager(), "merge-repo-dialog");
        mActivity.closeOperationDrawer();
    }

}
