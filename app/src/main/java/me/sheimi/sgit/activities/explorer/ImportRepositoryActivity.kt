package me.sheimi.sgit.activities.explorer

import android.app.Activity
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.InitLocalTask
import java.io.File
import java.io.FileFilter

class ImportRepositoryActivity : FileExplorerActivity() {

    override fun getRootFolder(): File = Environment.getExternalStorageDirectory()

    override fun getExplorerFileFilter(): FileFilter = FileFilter { it.isDirectory }

    override fun getScreenTitle(): String = getString(R.string.title_activity_import_repository)

    override fun onFileClick(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
        }
    }

    @Composable
    override fun TopBarActions() {
        Row {
            IconButton(onClick = { onCreateExternalClick() }) {
                Icon(Icons.Default.Add, contentDescription = getString(R.string.action_create_external_repo))
            }
            IconButton(onClick = { onImportExternalClick() }) {
                Icon(Icons.Default.SaveAlt, contentDescription = getString(R.string.label_import))
            }
        }
    }

    private fun onCreateExternalClick() {
        val dotGit = File(currentDir, Repo.DOT_GIT_DIR)
        if (dotGit.exists()) {
            showToastMessage(R.string.alert_is_already_a_git_repo)
            return
        }
        showMessageDialog(
            R.string.dialog_create_external_title,
            R.string.dialog_create_external_msg,
            R.string.dialog_create_external_positive_label
        ) { _, _ -> createExternalGitRepo() }
    }

    private fun onImportExternalClick() {
        val intent = Intent()
        intent.putExtra(RESULT_PATH, currentDir.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun createExternalGitRepo() {
        val localPath = Repo.EXTERNAL_PREFIX + currentDir
        val repo = Repo.createRepo(localPath, "local repository", getString(R.string.importing))
        InitLocalTask(repo).executeTask()
        finish()
    }
}
