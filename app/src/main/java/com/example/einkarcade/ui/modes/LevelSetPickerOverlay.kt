@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.einkarcade.R
import com.example.einkarcade.catalog.LevelSetSummary

@Composable
fun LevelSetPickerOverlay(
    setOptions: List<LevelSetSummary>,
    selectedSetId: Int?,
    onPickSet: (setId: Int) -> Unit,
    onImport: () -> Unit,
    onRename: (setId: Int, title: String) -> Unit,
    onDelete: (setId: Int) -> Unit,
    errorTitle: String?,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }

    var setBeingRenamed by remember { mutableStateOf<LevelSetSummary?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var setBeingDeleted by remember { mutableStateOf<LevelSetSummary?>(null) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    colorFilter = ColorFilter.tint(Color.LightGray),
                    modifier =
                        Modifier
                            .width(48.dp)
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Select Set",
                        fontSize = 20.sp,
                        color = Color.LightGray,
                    )
                }

                Text(
                    text = "Import",
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    modifier =
                        Modifier
                            .height(40.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable(onClick = onImport)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                )
            }

            if (setOptions.isEmpty()) {
                Text(
                    text = "No level sets",
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                ) {
                    items(setOptions, key = { it.id }) { set ->
                        val isSelected = set.id == selectedSetId
                        LevelSetCard(
                            setName = set.name,
                            completedCount = set.completedCount,
                            levelCount = set.levelCount,
                            isSelected = isSelected,
                            onClick = {
                                onPickSet(set.id)
                                onDismiss()
                            },
                            onRename = {
                                setBeingRenamed = set
                                renameTitle = set.name
                            },
                            onDelete = { setBeingDeleted = set },
                        )
                    }
                }
            }
        }

        setBeingRenamed?.let { set ->
            AlertDialog(
                onDismissRequest = { setBeingRenamed = null },
                title = { Text("Rename level set") },
                text = {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = renameTitle.isNotBlank(),
                        onClick = {
                            onRename(set.id, renameTitle)
                            setBeingRenamed = null
                        },
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setBeingRenamed = null }) {
                        Text("Cancel")
                    }
                },
            )
        }

        setBeingDeleted?.let { set ->
            AlertDialog(
                onDismissRequest = { setBeingDeleted = null },
                title = { Text("Delete level set?") },
                text = { Text("Delete ${set.name} and all of its levels?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(set.id)
                            setBeingDeleted = null
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setBeingDeleted = null }) {
                        Text("Cancel")
                    }
                },
            )
        }

        errorTitle?.let { title ->
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(title) },
                text = { Text(errorMessage.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

@Composable
private fun LevelSetCard(
    setName: String,
    completedCount: Int,
    levelCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val borderColor = if (isSelected) Color.LightGray else Color(0xFF7A7A7A)
    val borderWidth = if (isSelected) 3.dp else 2.dp
    val cardShape = RoundedCornerShape(8.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(width = borderWidth, color = borderColor, shape = cardShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = setName,
            fontSize = 18.sp,
            color = Color.LightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$completedCount/$levelCount",
            fontSize = 14.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(end = 24.dp),
        )
        Image(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = "Rename",
            colorFilter = ColorFilter.tint(Color.LightGray),
            modifier =
                Modifier
                    .width(36.dp)
                    .clickable(onClick = onRename)
                    .padding(8.dp),
        )
        Image(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = "Delete",
            colorFilter = ColorFilter.tint(Color.LightGray),
            modifier =
                Modifier
                    .width(36.dp)
                    .clickable(onClick = onDelete)
                    .padding(8.dp),
        )
    }
}
