package me.sheimi.sgit.activities.explorer

import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import java.io.File
import java.io.FileFilter

class ExploreRootDirActivity : FileExplorerActivity() {

    override fun getRootFolder(): File = Environment.getExternalStorageDirectory()

    override fun getExplorerFileFilter(): FileFilter =
        FileFilter { file -> !file.name.startsWith(".") && file.isDirectory }

    override fun getScreenTitle(): String = getString(R.string.title_activity_explore_file)

    override fun onFileClick(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
        }
    }

    @Composable
    override fun TopBarActions() {
        IconButton(onClick = {
            Repo.setLocalRepoRoot(this, currentDir)
            finish()
        }) {
            Icon(Icons.Default.Check, contentDescription = getString(R.string.action_select_root_dir))
        }
    }
}
