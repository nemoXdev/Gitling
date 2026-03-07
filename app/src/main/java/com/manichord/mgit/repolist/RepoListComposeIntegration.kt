package com.manichord.mgit.repolist

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.clone.CloneViewModel
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.activities.UserSettingsActivity
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.models.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListComposeContent(
    activity: SheimiFragmentActivity,
    cloneViewModel: CloneViewModel,
    viewModel: RepoListViewModel,
    onCloneClick: () -> Unit,
    onCancelCloneViewClick: () -> Unit
) {
    AppTheme {
        val repoListSnapshot by viewModel.repoList.collectAsState()
        val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
        var showCloneSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()

        RepoListScreen(
            repoList = repoListSnapshot,
            onRepoClick = { repo ->
                val intent = Intent(activity, RepoDetailActivity::class.java)
                intent.putExtra(Repo.TAG, repo)
                activity.startActivity(intent)
            },
                onRepoLongClick = { repo ->
                    if (repo.repoStatus == RepoContract.REPO_STATUS_NULL) {
                        showRepoOptionsDialog(activity, repo)
                    } else {
                        // Cancel the operational repo
                        repo.deleteRepo()
                        repo.cancelTask()
                    }
                },
                onCloneClick = {
                    showCloneSheet = true
                },
                onSearchClick = {
                    // TODO: Implement search
                },
                onSettingsClick = {
                    val intent = Intent(activity, UserSettingsActivity::class.java)
                    activity.startActivity(intent)
                }
            )

            if (showPermissionDialog) {
                PermissionRequiredDialog(
                    onConfirm = {
                        viewModel.setShowPermissionDialog(false)
                        val uri = Uri.fromParts("package", activity.packageName, null)
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                        activity.startActivity(intent)
                    },
                    onDismiss = {
                        viewModel.setShowPermissionDialog(false)
                        activity.finish()
                    }
                )
            }

            if (showCloneSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showCloneSheet = false
                        onCancelCloneViewClick()
                    },
                    sheetState = sheetState
                ) {
                    CloneView(
                        viewModel = cloneViewModel,
                        onCloneClick = {
                            showCloneSheet = false
                            onCloneClick()
                        },
                        onCancelClick = {
                            showCloneSheet = false
                            onCancelCloneViewClick()
                        }
                    )
                }
            }
        }
}

@Composable
fun PermissionRequiredDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_access_all_files_title)) },
        text = { Text(stringResource(R.string.dialog_access_all_files_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.label_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    )
}

private fun showRepoOptionsDialog(context: SheimiFragmentActivity, repo: Repo) {
    // Delegate to the exact same logic that adapter used, but we need to reimplement the dialogs
    // For a minimal bridge, we reproduce the dialogs here
    val options = context.resources.getStringArray(R.array.dialog_choose_repo_action_items)

    val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
        .setTitle(R.string.dialog_choose_option)
        .setItems(options) { dialogInterface: DialogInterface, which: Int ->
            when (which) {
                0 -> { // Rename
                    context.showEditTextDialog(
                        R.string.dialog_rename_repo_title,
                        R.string.dialog_rename_repo_hint,
                        R.string.label_rename,
                        object : SheimiFragmentActivity.OnEditTextDialogClicked {
                            override fun onClicked(text: String?) {
                                if (text != null && !repo.renameRepo(text)) {
                                    context.showToastMessage(R.string.error_rename_repo_fail)
                                }
                            }
                        }
                    )
                }
                1 -> { // Delete
                    context.showMessageDialog(
                        R.string.dialog_delete_repo_title,
                        R.string.dialog_delete_repo_msg,
                        R.string.label_delete,
                        { _: DialogInterface, _: Int ->
                            repo.deleteRepo()
                            repo.cancelTask()
                        }
                    )
                }
            }
        }
        .create()
    dialog.show()
}
