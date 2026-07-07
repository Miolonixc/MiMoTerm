package com.mimoterm.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimoterm.core.terminal.TerminalSession
import com.mimoterm.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val terminalSession = remember {
        TerminalSession(
            command = "/system/bin/sh",
            rows = 24,
            cols = 80
        )
    }

    // Auto-scroll to bottom
    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            terminalSession.stop()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Terminal",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                outputLines = listOf("Starting terminal session...")
                                terminalSession.start()
                                outputLines = outputLines + "Terminal started. Type commands below."
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isRunning) {
                                terminalSession.stop()
                                isRunning = false
                                outputLines = outputLines + "Session terminated."
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop"
                        )
                    }
                    IconButton(
                        onClick = {
                            outputLines = emptyList()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear"
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
            // Terminal output
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                command = command + "\t"
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Tab",
                                fontSize = 10.sp
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                // TODO: Command history
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                // TODO: Ctrl key modifier
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Ctrl",
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Command input
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        Key.Enter -> {
                                            if (command.isNotEmpty()) {
                                                outputLines = outputLines + "$ $command"
                                                terminalSession.sendCommand(command)
                                                command = ""
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            // TODO: Command history navigation
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            // TODO: Command history navigation
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                        placeholder = {
                            Text(
                                text = "Type command...",
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    IconButton(
                        onClick = {
                            if (command.isNotEmpty()) {
                                outputLines = outputLines + "$ $command"
                                terminalSession.sendCommand(command)
                                command = ""
                            }
                        },
                        enabled = command.isNotEmpty() && isRunning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (command.isNotEmpty() && isRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
