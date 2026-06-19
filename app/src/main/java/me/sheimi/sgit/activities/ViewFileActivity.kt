package me.sheimi.sgit.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.manichord.mgit.ui.components.FragmentHost
import com.manichord.mgit.ui.theme.AppTheme
import com.manichord.mgit.viewfile.ViewFileScreen
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.FsUtils
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.ChooseLanguageDialog
import me.sheimi.sgit.fragments.CommitsFragment
import me.sheimi.sgit.fragments.ViewFileFragment
import java.io.File

class ViewFileActivity : SheimiFragmentActivity() {

    companion object {
        const val TAG_FILE_NAME = "file_name"
        const val TAG_MODE = "mode"
        const val TAG_MODE_NORMAL: Short = 0
        const val TAG_MODE_SSH_KEY: Short = 1
    }

    private lateinit var fileFragment: ViewFileFragment
    private var commitsFragment: CommitsFragment? = null
    private var activityMode: Short = TAG_MODE_NORMAL

    private var currentTab by mutableStateOf(0)
    private var searchActive by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = IntentCompat.getSerializableExtra(intent, Repo.TAG, Repo::class.java)
        val extras = intent.extras!!
        val fileName = extras.getString(TAG_FILE_NAME)!!
        activityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL)

        val fileArgs = Bundle()
        fileArgs.putString(TAG_FILE_NAME, fileName)
        fileArgs.putShort(TAG_MODE, activityMode)
        fileFragment = ViewFileFragment()
        fileFragment.arguments = fileArgs

        if (repo != null) {
            commitsFragment = CommitsFragment.newInstance(repo, FsUtils.getRelativePath(File(fileName), repo.dir))
        }

        val screenTitle = File(fileName).name

        setContent {
            AppTheme {
                ViewFileScreen(
                    title = screenTitle,
                    hasCommitsTab = commitsFragment != null,
                    activityMode = activityMode,
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onBackClick = { finish() },
                    onEditClick = {
                        if (activityMode != TAG_MODE_SSH_KEY) {
                            FsUtils.openFile(this, fileFragment.file)
                        }
                    },
                    onChooseLanguageClick = {
                        if (activityMode != TAG_MODE_SSH_KEY) {
                            ChooseLanguageDialog().show(supportFragmentManager, "choose language")
                        }
                    },
                    onCopyAllClick = { fileFragment.copyAll() },
                    searchActive = searchActive,
                    onSearchActiveChange = { active ->
                        searchActive = active
                        if (!active) {
                            searchQuery = ""
                            commitsFragment?.setFilter(null)
                        }
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        commitsFragment?.setFilter(query)
                    },
                    fileContent = { FragmentHost(supportFragmentManager, fileFragment) },
                    commitsContent = {
                        commitsFragment?.let { FragmentHost(supportFragmentManager, it) }
                    }
                )
            }
        }
    }

    fun setLanguage(lang: String) {
        fileFragment.setLanguage(lang)
    }
}
