package com.manichord.mgit.repodetail

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.sheimi.android.utils.BasicFunctions
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.revwalk.RevCommit
import java.text.DateFormat

sealed class CommitRowState {
    data object Loading : CommitRowState()
    data class Item(val position: Int, val commit: RevCommit, val selected: Boolean) : CommitRowState()
}

@Composable
fun CommitsListContent(
    rows: List<CommitRowState>,
    dateFormatter: DateFormat,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(rows, key = { row -> if (row is CommitRowState.Item) row.commit.name else "loading" }) { row ->
            when (row) {
                is CommitRowState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is CommitRowState.Item -> {
                    CommitRow(
                        commit = row.commit,
                        selected = row.selected,
                        dateFormatter = dateFormatter,
                        onClick = { onItemClick(row.position) },
                        onLongClick = { onItemLongClick(row.position) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommitRow(
    commit: RevCommit,
    selected: Boolean,
    dateFormatter: DateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val person = commit.authorIdent
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AndroidView(
            factory = { context -> ImageView(context) },
            update = { imageView -> BasicFunctions.setAvatarImage(imageView, person.emailAddress) },
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = Repo.getCommitDisplayName(commit.name),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = commit.shortMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormatter.format(person.getWhen()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
