package me.sheimi.sgit.activities.explorer

import android.app.Activity
import android.content.Intent
import android.os.Environment
import me.sheimi.sgit.R
import java.io.File
import java.io.FileFilter

class ExploreFileActivity : FileExplorerActivity() {

    override fun getRootFolder(): File = Environment.getExternalStorageDirectory()

    override fun getExplorerFileFilter(): FileFilter =
        FileFilter { file -> !file.name.startsWith(".") }

    override fun getScreenTitle(): String = getString(R.string.title_activity_explore_file)

    override fun onFileClick(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
            return
        }
        val intent = Intent()
        intent.putExtra(RESULT_PATH, file.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
