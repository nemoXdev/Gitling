package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Loads commits with full branch/merge topology (lane assignment) via JGit's
 * {@link PlotWalk}, the same graph-layout engine EGit uses -- as opposed to
 * {@link GetCommitTask}'s plain linear history.
 */
public class GetCommitGraphTask extends RepoOpTask {

    private static final int MAX_COMMITS = 2000;

    public interface GetCommitGraphCallback {
        void postGraph(PlotCommitList<PlotLane> commits);
    }

    private final GetCommitGraphCallback mCallback;
    private final boolean mAllBranches;
    private PlotCommitList<PlotLane> mResult;

    public GetCommitGraphTask(Repo repo, boolean allBranches, GetCommitGraphCallback callback) {
        super(repo, false);
        mAllBranches = allBranches;
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return loadGraph();
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.postGraph(isSuccess ? mResult : null);
        }
    }

    private boolean loadGraph() {
        try {
            Repository repository = mRepo.getGit().getRepository();
            try (PlotWalk walk = new PlotWalk(repository)) {
                if (mAllBranches) {
                    for (Ref ref : repository.getRefDatabase()
                            .getRefsByPrefix("refs/heads/", "refs/remotes/")) {
                        Object obj = walk.parseAny(ref.getObjectId());
                        if (obj instanceof RevCommit) {
                            walk.markStart((RevCommit) obj);
                        }
                    }
                } else {
                    RevCommit head = walk.parseCommit(repository.resolve("HEAD"));
                    walk.markStart(head);
                }
                PlotCommitList<PlotLane> plotList = new PlotCommitList<>();
                plotList.source(walk);
                plotList.fillTo(MAX_COMMITS);
                mResult = plotList;
            }
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }
}
