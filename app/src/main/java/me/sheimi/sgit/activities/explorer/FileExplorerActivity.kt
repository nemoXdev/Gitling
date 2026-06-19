package me.sheimi.sgit.activities.explorer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.manichord.mgit.explorer.FileExplorerScreen
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import java.io.File
import java.io.FileFilter

abstract class FileExplorerActivity : SheimiFragmentActivity() {

    companion object {
        const val RESULT_PATH = "result_path"
    }

    private lateinit var resolvedRoot: File

    private var currentDirState by mutableStateOf<File?>(null)
    private var filesState by mutableStateOf<List<File>>(emptyList())
    protected var selectedFile by mutableStateOf<File?>(null)

    protected val currentDir: File
        get() = currentDirState ?: resolvedRoot

    protected abstract fun getRootFolder(): File
    protected abstract fun getExplorerFileFilter(): FileFilter?
    protected abstract fun onFileClick(file: File)
    protected open fun onFileLongClick(file: File) {}
    protected open fun getScreenTitle(): String = ""

    @Composable
    protected open fun TopBarActions() {}

    /** Optional overlay (e.g. a bottom sheet of actions for [selectedFile]) drawn on top of the list. */
    @Composable
    protected open fun Overlay() {}

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resolvedRoot = getRootFolder()
        currentDirState = resolvedRoot
        loadFiles()

        // Higher priority than SheimiFragmentActivity's generic "just finish" callback
        // (registered in its onCreate, called before this one): go up a directory first
        // if we're not already at the root, only finishing once there's nowhere left to go.
        onBackPressedDispatcher.addCallback(this) {
            val parent = currentDir.parentFile
            if (currentDir != resolvedRoot && parent != null) {
                setCurrentDir(parent)
            } else {
                finish()
            }
        }

        setContent {
            AppTheme {
                Box {
                    FileExplorerScreen(
                        title = getScreenTitle(),
                        currentPath = currentDir.path,
                        files = filesState,
                        showUpRow = currentDir.parentFile != null,
                        onUpClick = { currentDir.parentFile?.let { setCurrentDir(it) } },
                        onBackClick = { finish() },
                        onItemClick = { onFileClick(it) },
                        onItemLongClick = { onFileLongClick(it) },
                        selectedFile = selectedFile,
                        actions = { TopBarActions() }
                    )
                    Overlay()
                }
            }
        }
    }

    protected fun setCurrentDir(dir: File) {
        currentDirState = dir
        loadFiles()
    }

    fun refreshList() {
        loadFiles()
    }

    private fun loadFiles() {
        val filter = getExplorerFileFilter()
        val files = (if (filter != null) currentDir.listFiles(filter) else currentDir.listFiles())
            ?: emptyArray()
        filesState = files.sortedWith(
            compareBy({ !it.isDirectory }, { it.toString() })
        )
    }
}
