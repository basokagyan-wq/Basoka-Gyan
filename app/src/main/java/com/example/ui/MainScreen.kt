package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.ui.components.ApiKeySheet
import com.example.ui.components.AssistantOrb
import com.example.ui.components.BasokaAnimatedBackground
import com.example.ui.components.BasokaEmptyStateWelcome
import com.example.ui.components.ChatBubble
import com.example.ui.components.PermissionSheet
import com.example.ui.components.ScreenReaderSheet
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val messages by viewModel.messages.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val isHandsFree by viewModel.isHandsFreeMode.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val screenData by viewModel.screenData.collectAsState()
    val showPermissionSheet by viewModel.showPermissionSheet.collectAsState()
    val showScreenReaderSheet by viewModel.showScreenReaderSheet.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val hasAutomaticKey by viewModel.hasAutomaticKey.collectAsState()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Single permission launcher for runtime permissions (Mic, Camera, Contacts, etc.)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            viewModel.setImageForAnalysis(bitmap)
        }
    }

    // Scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Kurdish is an RTL language; provide RTL layout direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack),
            containerColor = CyberBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (assistantState == AssistantState.IDLE) EmeraldGreen else ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "BASOKA",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "بەڵگە و سەرچاوە",
                                            color = ElectricCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "💎 بێسنوور",
                                            color = EmeraldGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "گفتوگۆی نەرم • بەڵگەی ورد • بەکارهێنانی بێسنوور",
                                    color = CyanGlow,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkNavy.copy(alpha = 0.95f)
                    ),
                    actions = {
                        // Screen Reader
                        IconButton(
                            onClick = { viewModel.toggleScreenReaderSheet(true) },
                            modifier = Modifier.testTag("screen_reader_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "خوێندنەوەی شاشە",
                                tint = ElectricCyan
                            )
                        }

                        // Hands-free toggle
                        IconButton(
                            onClick = { viewModel.toggleHandsFree() },
                            modifier = Modifier.testTag("hands_free_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "دۆخی بێدەست",
                                tint = if (isHandsFree) EmeraldGreen else TextMuted
                            )
                        }

                        // Permissions
                        IconButton(
                            onClick = { viewModel.togglePermissionSheet(true) },
                            modifier = Modifier.testTag("permissions_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "مۆڵەتەکان",
                                tint = CyanGlow
                            )
                        }

                        // API Key Settings
                        IconButton(
                            onClick = { viewModel.toggleApiKeyDialog(true) },
                            modifier = Modifier.testTag("api_key_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "ڕێکخستنی کلیل",
                                tint = if (customApiKey.isNotBlank()) ElectricCyan else TextSecondary
                            )
                        }

                        // Clear history
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_history_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "سڕینەوەی مێژوو",
                                tint = TextMuted
                            )
                        }
                    },
                    modifier = Modifier.border(0.5.dp, SurfaceBorder)
                )
            },
            bottomBar = {
                BottomInteractionBar(
                    textInput = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank() || selectedBitmap != null) {
                            viewModel.processUserInput(textInput)
                            textInput = ""
                        }
                    },
                    onMicClick = {
                        if (assistantState == AssistantState.LISTENING) {
                            viewModel.stopVoiceInput()
                        } else {
                            viewModel.startVoiceInput()
                        }
                    },
                    onPickImage = { photoPickerLauncher.launch("image/*") },
                    selectedBitmap = selectedBitmap,
                    onRemoveImage = { viewModel.setImageForAnalysis(null) },
                    assistantState = assistantState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Futuristic AI Core Section
                AiCoreHeader(
                    assistantState = assistantState,
                    audioRms = audioRms,
                    statusText = statusText,
                    onOrbClick = {
                        if (assistantState == AssistantState.LISTENING) {
                            viewModel.stopVoiceInput()
                        } else if (assistantState == AssistantState.SPEAKING) {
                            viewModel.cancelCurrentAction()
                        } else {
                            viewModel.startVoiceInput()
                        }
                    }
                )

                // Quick Kurdish Suggestions
                QuickCommandChips(
                    onCommandSelected = { commandText ->
                        viewModel.processUserInput(commandText)
                    }
                )

                // Conversation Area with Intelligent Animated "basoka" Background
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Animated intelligent background with "basoka" watermark, neural nodes and pulses
                    BasokaAnimatedBackground(
                        assistantState = assistantState,
                        audioRms = audioRms,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Empty state welcome when there are no messages yet
                    if (messages.isEmpty()) {
                        BasokaEmptyStateWelcome(
                            assistantState = assistantState,
                            onCommandSelected = { commandText ->
                                viewModel.processUserInput(commandText)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    // Conversation Message List rendered over the animated background
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                onSpeakAgain = { text -> viewModel.speakText(text) },
                                onConfirmAction = { msg -> viewModel.confirmPendingAction(msg) },
                                onCancelAction = { msg -> viewModel.cancelPendingAction(msg) },
                                onOpenRecoverySettings = {
                                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Permission Management Sheet
        if (showPermissionSheet) {
            PermissionSheet(
                permissions = permissions,
                onRequestPermission = { item ->
                    if (item.isSpecialSettings) {
                        val intent = viewModel.getSettingsIntent(item)
                        context.startActivity(intent)
                    } else if (item.androidPermission != null) {
                        permissionLauncher.launch(item.androidPermission)
                    }
                },
                onDismiss = { viewModel.togglePermissionSheet(false) }
            )
        }

        // Screen Reader Sheet
        if (showScreenReaderSheet) {
            ScreenReaderSheet(
                screenData = screenData,
                isAccessibilityEnabled = permissions.find { it.id == "accessibility" }?.isGranted == true,
                onEnableAccessibility = {
                    val accItem = permissions.find { it.id == "accessibility" }
                    if (accItem != null) {
                        context.startActivity(viewModel.getSettingsIntent(accItem))
                    }
                },
                onAskAiToSummarize = { screenText ->
                    viewModel.toggleScreenReaderSheet(false)
                    viewModel.processUserInput("دەقی ئەم شاشەیە بۆم کورت و ڕوون بکەرەوە بە کوردی سۆرانی:\n$screenText")
                },
                onCopyAll = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                },
                onDismiss = { viewModel.toggleScreenReaderSheet(false) }
            )
        }

        // API Key Configuration Sheet
        if (showApiKeyDialog) {
            ApiKeySheet(
                currentCustomKey = customApiKey,
                hasAutomaticKey = hasAutomaticKey,
                onSaveKey = { key -> viewModel.saveCustomApiKey(key) },
                onClearKey = { viewModel.clearCustomApiKey() },
                onDismiss = { viewModel.toggleApiKeyDialog(false) }
            )
        }
    }
}

@Composable
fun AiCoreHeader(
    assistantState: AssistantState,
    audioRms: Float,
    statusText: String,
    onOrbClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(DarkNavy.copy(alpha = 0.7f), CyberBlack)
                )
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AssistantOrb(
                state = assistantState,
                audioRms = audioRms,
                size = 92.dp,
                onClick = onOrbClick,
                modifier = Modifier.testTag("assistant_orb")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                color = when (assistantState) {
                    AssistantState.LISTENING -> ElectricCyan
                    AssistantState.THINKING -> NeonPurple
                    AssistantState.SPEAKING -> CyanGlow
                    AssistantState.AWAITING_CONFIRMATION -> com.example.ui.theme.AmberWarning
                    AssistantState.IDLE -> TextSecondary
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun QuickCommandChips(
    onCommandSelected: (String) -> Unit
) {
    val quickCommands = listOf(
        "💡 فلایشی مۆبایل داگیرسێنە",
        "📍 قەڵای هەولێر لەکوێیە؟ لۆکەیشنیم بۆ بنێرە",
        "👤 کێیە مام جەلال تاڵەبانی؟",
        "👤 شێرکۆ بێکەس کێیە و ئەکاونتەکانی بنێرە",
        "👤 کریستیانۆ ڕۆناڵدۆ و تۆڕە کۆمەڵایەتییەکانی",
        "📍 شوێنی پردی دەلال لە زاخۆ لەگەڵ نەخشە",
        "📍 بەنداوی دووکان و ناونیشانی دەقیق",
        "📍 قەڵای شێروانە لە کەلار لەکوێیە؟",
        "📍 ئەشکەوتی شانەدەر لەکوێیە؟",
        "📍 کەعبەی پیرۆز لەکوێیە؟ لۆکەیشنیم پێ بڵێ",
        "🔍 گەڕان لە گووگڵ بۆ نوێترین زانیاری و بەڵگە",
        "سڵاو برای بەڕێزم چۆنی؟ ڕەوشت چۆنە؟",
        "ڕاوێژێکم پێویستە دەربارەی کار و ژیان",
        "چۆن ڤایرۆس لە مۆبایل پاکبکەمەوە بە بەڵگەوە؟",
        "هۆکاری سەرئێشە چییە لەگەڵ سەرچاوەی پزیشکی؟",
        "Google Maps بکەرەوە",
        "Wi-Fi بکەرەوە",
        "کەمێک ڕووناکی شاشە زیاد بکە",
        "وێنەیەک بگرە",
        "مۆسیقا پەخش بکە",
        "دەقی شاشە بخوێنەوە",
        "دۆخی باتری و مۆبایل",
        "YouTube بکەرەوە",
        "کاتژمێر ٨ بیرم بخەرەوە",
        "Bluetooth بکەرەوە"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (cmd in quickCommands) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable { onCommandSelected(cmd) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = cmd,
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BottomInteractionBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onPickImage: () -> Unit,
    selectedBitmap: Bitmap?,
    onRemoveImage: () -> Unit,
    assistantState: AssistantState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2101B2E), Color(0xF80A101C))
                )
            )
            .border(
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            SurfaceBorder.copy(alpha = 0.4f),
                            ElectricCyan.copy(alpha = 0.5f),
                            NeonPurple.copy(alpha = 0.5f),
                            SurfaceBorder.copy(alpha = 0.4f)
                        )
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Selected Image Preview if any
            AnimatedVisibility(visible = selectedBitmap != null) {
                selectedBitmap?.let { bmp ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "وێنەی دیاریکراو",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وێنە هەڵبژێردرا بۆ ناسینەوەی کەس یان شوێن",
                            color = CyanGlow,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "سڕینەوەی وێنە",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery / Camera Attachment
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "هاوپێچکردنی وێنە بۆ ناسینەوەی کەس یان شوێن",
                        tint = CyanGlow
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            text = if (selectedBitmap != null) "پرسیارێک بنووسە یان بنێرە بۆ شیکارکردنی وێنە..." else "پرسیاری شوێنێک، ناسینەوەی کەسێک، یان فەرمانێک...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        cursorColor = ElectricCyan,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("command_text_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (textInput.isNotBlank() || selectedBitmap != null) {
                    // Send Button with glowing border
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onSend)
                            .testTag("send_command_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ناردنی فەرمان",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Big Futuristic Mic Button
                    val isListening = assistantState == AssistantState.LISTENING
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.radialGradient(listOf(NeonPurple, com.example.ui.theme.CrimsonRed))
                                else Brush.radialGradient(listOf(ElectricCyan, NeonPurple))
                            )
                            .border(1.5.dp, if (isListening) com.example.ui.theme.CrimsonRed else ElectricCyan, CircleShape)
                            .clickable(onClick = onMicClick)
                            .testTag("voice_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "وەستاندنی گوێگرتن" else "دەستپێکردنی گوێگرتن",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        }
    } catch (_: Exception) {
        null
    }
}
