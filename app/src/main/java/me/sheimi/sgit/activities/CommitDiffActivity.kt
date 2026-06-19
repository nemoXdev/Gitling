package me.sheimi.sgit.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import com.manichord.mgit.diff.CommitDiffScreen
import com.manichord.mgit.ui.theme.AppTheme
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import timber.log.Timber
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.CodeGuesser
import me.sheimi.android.utils.FsUtils
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.CommitDiffTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class CommitDiffActivity : SheimiFragmentActivity() {

    companion object {
        const val OLD_COMMIT = "old commit"
        const val NEW_COMMIT = "new commit"
        const val SHOW_DESCRIPTION = "show_description"
        private const val JS_INTERFACE = "CodeLoader"

        private const val HTML_TMPL = "<!doctype html>" +
            "<head>" +
            " <script src=\"js/jquery.js\"></script>" +
            " <script src=\"js/highlight.pack.js\"></script>" +
            " <script src=\"js/local_commits_diff.js\"></script>" +
            " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/rainbow.css\" />" +
            " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/local_commits_diff.css\" />" +
            "</head><body></body>"
    }

    private var oldCommit: String? = null
    private lateinit var newCommit: String
    private var showDescription: Boolean = false
    private var repo: Repo? = null
    private var commit: RevCommit? = null
    private var diffStrs: List<String> = emptyList()
    private var diffEntries: List<DiffEntry> = emptyList()

    private var isLoading by mutableStateOf(true)
    private var webView: WebView? = null

    private val saveDiffLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val diffUri = result.data!!.data
            if (diffUri != null) {
                try {
                    contentResolver.openOutputStream(diffUri)?.use { saveDiff(it) }
                } catch (e: IOException) {
                    showToastMessage(R.string.alert_file_creation_failure)
                }
            }
        }
    }

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras!!
        oldCommit = extras.getString(OLD_COMMIT)
        newCommit = extras.getString(NEW_COMMIT)!!
        showDescription = extras.getBoolean(SHOW_DESCRIPTION)
        repo = IntentCompat.getSerializableExtra(intent, Repo.TAG, Repo::class.java)

        var title = Repo.getCommitDisplayName(newCommit)
        oldCommit?.let { title += " : " + Repo.getCommitDisplayName(it) }
        val screenTitle = getString(R.string.title_activity_commit_diff) + title

        setContent {
            AppTheme {
                CommitDiffScreen(
                    title = screenTitle,
                    isLoading = isLoading,
                    onBackClick = { finish() },
                    onShareClick = { shareDiff() },
                    onSaveClick = { launchSaveDiff() },
                    onWebViewCreated = { setupWebView(it) }
                )
            }
        }
    }

    private fun setupWebView(view: WebView) {
        if (webView != null) return // AndroidView's factory should only run once; guard anyway
        webView = view
        view.addJavascriptInterface(CodeLoader(), JS_INTERFACE)
        view.settings.javaScriptEnabled = true
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Timber.d(
                    "%s -- From line %d of %s",
                    consoleMessage.message(), consoleMessage.lineNumber(), consoleMessage.sourceId()
                )
                return true
            }
        }
        view.setBackgroundColor(Color.TRANSPARENT)
        view.loadDataWithBaseURL("file:///android_asset/", HTML_TMPL, "text/html", "utf-8", null)
    }

    private fun formatCommitInfo(): String {
        val c = commit ?: return ""
        val committer = c.committerIdent
        val author = c.authorIdent
        return "commit $newCommit\n" +
            "Author:     ${author.name} <${author.emailAddress}>\n" +
            "AuthorDate: ${author.whenAsInstant}\n" +
            "Commit:     ${committer.name} <${committer.emailAddress}>\n" +
            "CommitDate: ${committer.whenAsInstant}\n"
    }

    private fun saveDiff(fos: OutputStream) {
        commit?.let { c ->
            fos.write(formatCommitInfo().toByteArray())
            fos.write("\n".toByteArray())
            for (line in c.fullMessage.split("\n")) {
                fos.write("    $line\n".toByteArray())
            }
            fos.write("\n".toByteArray())
        }
        for (str in diffStrs) {
            fos.write(str.toByteArray())
        }
    }

    private fun sharedDiffPathName(): File {
        var fname = newCommit
        oldCommit?.let { fname += "_$it" }
        return File(FsUtils.getExternalDir("diff", true), "$fname.diff")
    }

    private fun shareDiff() {
        try {
            val diff = sharedDiffPathName()
            FileOutputStream(diff).use { saveDiff(it) }
            // A raw file:// Uri triggers FileUriExposedException on API 24+ -- share via
            // the app's existing FileProvider instead (already used by FsUtils.openFile()).
            val diffUri = FileProvider.getUriForFile(this, FsUtils.PROVIDER_AUTHORITY, diff)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-patch"
                putExtra(Intent.EXTRA_STREAM, diffUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        } catch (e: IOException) {
            showToastMessage(R.string.alert_file_creation_failure)
        }
    }

    private fun launchSaveDiff() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("text/x-patch")
            .putExtra(Intent.EXTRA_TITLE, "${Repo.getCommitDisplayName(newCommit)}.diff")
        saveDiffLauncher.launch(intent)
    }

    private inner class CodeLoader {
        @JavascriptInterface
        fun getDiff(index: Int): String = diffStrs[index]

        @JavascriptInterface
        fun haveCommitInfo(): Boolean = commit != null

        @JavascriptInterface
        fun getCommitInfo(): String = formatCommitInfo()

        @JavascriptInterface
        fun getCommitMessage(): String = commit?.fullMessage ?: ""

        @JavascriptInterface
        fun getChangeType(index: Int): String = diffEntries[index].changeType.toString()

        @JavascriptInterface
        fun getOldPath(index: Int): String = diffEntries[index].oldPath

        @JavascriptInterface
        fun getNewPath(index: Int): String = diffEntries[index].newPath

        @JavascriptInterface
        fun getDiffEntries() {
            val effectiveOldCommit = oldCommit ?: "$newCommit^"
            CommitDiffTask(repo, effectiveOldCommit, newCommit, { entries, strs, c ->
                diffEntries = entries
                diffStrs = strs
                commit = c
                isLoading = false
                webView?.loadUrl(CodeGuesser.wrapUrlScript("notifyEntriesReady();"))
            }, showDescription).executeTask()
        }

        @JavascriptInterface
        fun getDiffSize(): Int = diffEntries.size

        @JavascriptInterface
        fun getTheme(): String = Profile.getCodeMirrorTheme(applicationContext)
    }
}
