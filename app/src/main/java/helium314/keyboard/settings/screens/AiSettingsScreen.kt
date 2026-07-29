// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.AiAction
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.BackButton
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark
import helium314.keyboard.settings.initPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var actions by remember { mutableStateOf(AiAction.loadActions(ctx)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<AiAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_assist)) },
                navigationIcon = { BackButton(onClickBack) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ai_assist_add))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ai_assist_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            var token by remember { mutableStateOf(prefs.getString("pref_groq_token", "") ?: "") }
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; prefs.edit { putString("pref_groq_token", it) } },
                label = { Text(stringResource(R.string.ai_groq_token)) },
                placeholder = { Text("gsk_...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (actions.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.ai_assist_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(stringResource(R.string.ai_assist_add))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(actions, key = { it.id }) { action ->
                        AiActionCard(
                            action = action,
                            onEdit = { editingAction = action },
                            onDelete = {
                                actions = actions.filter { it.id != action.id }
                                AiAction.saveActions(ctx, actions)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AiActionEditDialog(
            action = null,
            onDismiss = { showAddDialog = false },
            onSave = { newAction ->
                actions = actions + newAction
                AiAction.saveActions(ctx, actions)
                showAddDialog = false
            }
        )
    }

    editingAction?.let { action ->
        AiActionEditDialog(
            action = action,
            onDismiss = { editingAction = null },
            onSave = { updated ->
                actions = actions.map { if (it.id == updated.id) updated else it }
                AiAction.saveActions(ctx, actions)
                editingAction = null
            }
        )
    }
}

@Composable
private fun AiActionCard(
    action: AiAction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = action.prompt.take(100) + if (action.prompt.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ai_assist_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ai_assist_delete))
            }
        }
    }
}

@Composable
private fun AiActionEditDialog(
    action: AiAction?,
    onDismiss: () -> Unit,
    onSave: (AiAction) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(action?.name ?: "") }
    var prompt by remember { mutableStateOf(action?.prompt ?: "") }
    var iconName by remember { mutableStateOf(action?.iconName ?: "edit") }
    var needsInput by remember { mutableStateOf(action?.needsInput ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (action == null) stringResource(R.string.ai_assist_add) else stringResource(R.string.ai_assist_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ai_assist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.ai_assist_prompt)) },
                    placeholder = { Text(stringResource(R.string.ai_assist_prompt_example)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                IconPicker(selectedIcon = iconName, onIconSelected = { iconName = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = needsInput,
                        onCheckedChange = { needsInput = it }
                    )
                    Text(
                        text = stringResource(R.string.ai_assist_needs_input),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && prompt.isNotBlank()) {
                        val newAction = AiAction(
                            id = action?.id ?: "ai_${System.currentTimeMillis()}",
                            name = name.trim(),
                            prompt = prompt.trim(),
                            iconName = iconName,
                            keyCode = action?.keyCode ?: AiAction.getNextCustomKeyCode(context),
                            needsInput = needsInput
                        )
                        onSave(newAction)
                    }
                },
                enabled = name.isNotBlank() && prompt.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun IconPicker(selectedIcon: String, onIconSelected: (String) -> Unit) {
    val icons = listOf(
        "edit", "autocorrect", "plus", "delete", "undo", "redo", "copy", "paste", "cut",
        "settings", "voice", "clipboard", "emoji", "dpad", "search"
    )
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedIcon,
            onValueChange = {},
            label = { Text(stringResource(R.string.ai_assist_icon)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            icons.forEach { icon ->
                DropdownMenuItem(
                    text = { Text(icon) },
                    onClick = {
                        onIconSelected(icon)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            AiSettingsScreen { }
        }
    }
}

fun createAiSettings(context: Context) = listOf(
    Setting(context, "screen_ai_assist", R.string.ai_assist, R.string.ai_assist_summary) { setting ->
        AiSettingsScreen { }
    }
)
