package com.manichord.mgit.repolist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import java.util.Collections

class RepoListViewModel(application: Application) : AndroidViewModel(application), RepoDbManager.RepoDbObserver {

    private val _repoList = MutableStateFlow<List<Repo>>(emptyList())
    val repoList: StateFlow<List<Repo>> = _repoList.asStateFlow()

    init {
        RepoDbManager.registerDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
        refreshRepoList()
    }

    fun refreshRepoList() {
        viewModelScope.launch {
            val cursor = RepoDbManager.queryAllRepo()
            val repos = Repo.getRepoList(getApplication(), cursor)
            Collections.sort(repos)
            cursor.close()
            _repoList.value = repos
        }
    }

    override fun nofityChanged() {
        refreshRepoList()
    }

    override fun onCleared() {
        super.onCleared()
        RepoDbManager.unregisterDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
    }
}
