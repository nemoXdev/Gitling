package com.manichord.mgit.repolist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.manichord.mgit.clone.CloneViewModel
import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneView(
    viewModel: CloneViewModel,
    onCloneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remoteUrl is a plain String in the VM for now, we sync it manually
    var remoteUrl by remember { mutableStateOf(viewModel.remoteUrl) }
    val localRepoName by viewModel.localRepoName.observeAsState("")
    val initLocal by viewModel.initLocal.observeAsState(false)
    val cloneRecursively by remember { mutableStateOf(viewModel.cloneRecursively) }

    val remoteUrlError by viewModel.remoteUrlError.observeAsState()
    val localRepoNameError by viewModel.localRepoNameError.observeAsState()

    val cloneLocation by viewModel.cloneLocation.observeAsState("")
    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.cloneLocation.value = it.toString() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp) // Extra padding for bottom sheet
    ) {
        Text(
            text = if (initLocal) stringResource(id = R.string.dialog_clone_neutral_label) else stringResource(id = R.string.title_clone_repo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!initLocal) {
            val accounts by viewModel.accounts.observeAsState(emptyList())
            val selectedAccount by viewModel.selectedAccount.observeAsState()
            var expanded by remember { mutableStateOf(false) }

            if (accounts.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account (Optional)",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Git Account") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Manual Credentials)") },
                            onClick = {
                                viewModel.selectedAccount.value = null
                                expanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.username})") },
                                onClick = {
                                    viewModel.selectedAccount.value = account
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = remoteUrl,
                onValueChange = {
                    remoteUrl = it
                    viewModel.remoteUrl = it  // sync to VM
                },
                label = { Text(stringResource(id = R.string.label_remote_url)) },
                placeholder = { Text("https://github.com/user/repo.git") },
                modifier = Modifier.fillMaxWidth(),
                isError = !remoteUrlError.isNullOrEmpty(),
                supportingText = {
                    if (!remoteUrlError.isNullOrEmpty()) {
                        Text(text = remoteUrlError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = localRepoName,
            onValueChange = { viewModel.localRepoName.value = it },
            label = { Text(stringResource(id = R.string.dialog_clone_local_path_hint)) },
            modifier = Modifier.fillMaxWidth(),
            isError = !localRepoNameError.isNullOrEmpty(),
            supportingText = {
                if (!localRepoNameError.isNullOrEmpty()) {
                    Text(text = localRepoNameError!!)
                } else {
                    Text(text = "Cloning to: $cloneLocation/$localRepoName")
                }
            },
            trailingIcon = {
                IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Change Parent Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = initLocal,
                        onCheckedChange = { viewModel.initLocal.value = it }
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_clone_neutral_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (!initLocal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = cloneRecursively,
                            onCheckedChange = { viewModel.cloneRecursively = it }
                        )
                        Text(
                            text = stringResource(id = R.string.dialog_clone_recursive),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(id = R.string.label_cancel))
            }
            Button(
                onClick = onCloneClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    if (initLocal) stringResource(id = R.string.label_init)
                    else stringResource(id = R.string.label_clone)
                )
            }
        }
    }
}
