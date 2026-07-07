package com.mimoterm.ui.screens.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimoterm.core.filemanager.FileManager
import com.mimoterm.core.filemanager.FileItem
import com.mimoterm.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onBack: () -> Unit
) {
    var currentPath by remember { mutableStateOf("/data/data/com.termux/files/home") }
    var files by remember { mutableStateOf(listOf<FileItem>()) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Load files when path changes
    LaunchedEffect(currentPath) {
        // In real implementation, use FileManager from DI
        files = listOf(
            FileItem("project", "$currentPath/project", true, 0, System.currentTimeMillis()),
            FileItem("build.gradle.kts", "$currentPath/build.gradle.kts", false, 1024, System.currentTimeMillis()),
            FileItem("src", "$currentPath/src", true, 0, System.currentTimeMillis()),
            FileItem("app", "$currentPath/app", true, 0, System.currentTimeMillis()),
            FileItem("README.md", "$currentPath/README.md", false, 512, System.currentTimeMillis()),
            FileItem(".gitignore", "$currentPath/.gitignore", false, 128, System.currentTimeMillis())
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Files",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            // TODO: Search
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(
                        onClick = {
                            showNewFolderDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs for Local/FTP/WebDAV
            TabRow(
                selectedTabIndex = selectedTab
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Local") },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("FTP") },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("WebDAV") },
                    icon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
            }

            // Current path breadcrumb
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = {
                            val parentPath = currentPath.substringBeforeLast('/')
                            if (parentPath.isNotEmpty()) {
                                currentPath = parentPath
                            }
                        },
                        enabled = currentPath != "/"
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Up"
                        )
                    }

                    // Path display
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }

            // File list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                currentPath = file.path
                            }
                        },
                        onShare = {
                            // TODO: Share file
                        },
                        onDelete = {
                            // TODO: Delete file
                        }
                    )
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotEmpty()) {
                            // TODO: Create folder
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewFolderDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileListItem(
    file: FileItem,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.extension in listOf("kt", "java") -> Icons.Default.Code
                    file.extension in listOf("md", "txt") -> Icons.Default.Description
                    file.extension in listOf("jpg", "png", "gif") -> Icons.Default.Image
                    file.extension in listOf("mp3", "wav") -> Icons.Default.AudioFile
                    file.extension in listOf("mp4", "avi") -> Icons.Default.VideoFile
                    file.extension in listOf("zip", "tar", "gz") -> Icons.Default.Archive
                    file.extension in listOf("json", "xml", "yaml") -> Icons.Default.DataObject
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More options
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
