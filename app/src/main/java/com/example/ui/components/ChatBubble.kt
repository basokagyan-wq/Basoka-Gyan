package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.model.ChatMessage
import com.example.model.ExecutionStatus
import com.example.model.GroundingSource
import com.example.model.PlaceLocation
import com.example.model.RecognizedPerson
import com.example.model.SocialAccount
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YellowBadgeBg
import com.example.ui.theme.YellowBorder
import com.example.ui.theme.YellowHighlight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakAgain: (String) -> Unit,
    onConfirmAction: (ChatMessage) -> Unit,
    onCancelAction: (ChatMessage) -> Unit,
    onOpenRecoverySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    var isExtraExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        if (!isUser) {
            // Basoka AI Avatar with glowing ring
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(ElectricCyan, NeonPurple, Color(0xFF0F172A)))
                    )
                    .border(1.5.dp, ElectricCyan.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "BASOKA AI",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
        ) {
            // Main bubble with cyber glassmorphism
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 4.dp else 18.dp,
                            bottomEnd = if (isUser) 18.dp else 4.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(
                            listOf(
                                Color(0xFF132847),
                                Color(0xFF1B365D),
                                Color(0xFF162D4D)
                            )
                        ) else Brush.linearGradient(
                            listOf(
                                Color(0xEE0E182A),
                                Color(0xF2152238)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = if (isUser) {
                            Brush.linearGradient(
                                listOf(
                                    ElectricCyan.copy(alpha = 0.6f),
                                    CyanGlow.copy(alpha = 0.25f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    NeonPurple.copy(alpha = 0.45f),
                                    ElectricCyan.copy(alpha = 0.35f),
                                    SurfaceBorder
                                )
                            )
                        },
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 4.dp else 18.dp,
                            bottomEnd = if (isUser) 18.dp else 4.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column {
                    // Header tag for AI message
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BASOKA AI",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            if (message.isDeepVerified) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YellowBadgeBg)
                                        .border(0.8.dp, YellowBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "پشتڕاستکراوە",
                                        tint = GoldenYellow,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "سەرچاوەی باوەڕپێکراو",
                                        color = GoldenYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Attached image if any
                    if (message.imageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = message.imageBitmap.asImageBitmap(),
                                contentDescription = "وێنەی هاوپێچکراو",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    // Formatted message text with Golden Yellow highlights for important items
                    FormattedKurdishMessageText(
                        text = message.text,
                        isUser = isUser
                    )

                    // Place / Location Card if place identified
                    if (!isUser && message.placeLocation != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PlaceLocationCard(location = message.placeLocation)
                    }

                    // Recognized Person Profile Card if person identified
                    if (!isUser && message.recognizedPerson != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PersonProfileCard(person = message.recognizedPerson)
                    }

                    // Grounding Sources and Evidence Citations
                    val allSources = (message.sources + (message.command?.sources ?: emptyList())).distinctBy { it.uri }
                    if (!isUser && allSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        EvidenceSourcesView(sources = allSources)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Footer with time, copy button, and speak button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedTime,
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy Text Button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Message", message.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "دەقەکە کۆپیکرا", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("copy_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کۆپیکردنی دەق",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Text-To-Speech Playback Button for AI
                            if (!isUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onSpeakAgain(message.text) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("speak_again_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "دووبارە دەنگ لێبدەوە",
                                        tint = CyanGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Structured Action Card if present
            if (message.command != null) {
                Spacer(modifier = Modifier.height(6.dp))
                ActionCard(
                    message = message,
                    onConfirm = { onConfirmAction(message) },
                    onCancel = { onCancelAction(message) },
                    onOpenSettings = onOpenRecoverySettings,
                    isExpanded = isExtraExpanded,
                    onToggleExpand = { isExtraExpanded = !isExtraExpanded }
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            // User Avatar with cyber styling
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F1E33)))
                    )
                    .border(1.5.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تۆ",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    message: ChatMessage,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val cmd = message.command ?: return

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("action_card_${cmd.intent}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Intent badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(YellowBadgeBg)
                        .border(0.8.dp, YellowBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "فەرمان: ${cmd.intent}",
                        color = GoldenYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status chip
                val (statusColor, statusText, statusIcon) = when (message.status) {
                    ExecutionStatus.SUCCESS -> Triple(EmeraldGreen, "سەرکەوتوو بوو", Icons.Default.CheckCircle)
                    ExecutionStatus.RUNNING -> Triple(ElectricCyan, "لە ئەنجامداندا...", Icons.Default.Info)
                    ExecutionStatus.PENDING_CONFIRMATION -> Triple(GoldenYellow, "چاوەڕوانی ڕەزامەندی", Icons.Default.Warning)
                    ExecutionStatus.CANCELLED -> Triple(TextMuted, "پەشیمان بووەوە", Icons.Default.Close)
                    ExecutionStatus.ERROR -> Triple(GoldenYellow, "مۆڵەت پێویستە", Icons.Default.Warning)
                    ExecutionStatus.IDLE -> Triple(CyanGlow, "ئامادەیە", Icons.Default.Info)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = cmd.kurdishSummary.ifBlank { "کرداری سیستەم" },
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            if (cmd.target.isNotBlank()) {
                Text(
                    text = "ئامانج: ${cmd.target}",
                    color = CyberYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Pending Confirmation Action Buttons
            if (message.status == ExecutionStatus.PENDING_CONFIRMATION) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ ئەم کردارە گۆڕانکاری گرنگ ئەنجام دەدات. دڵنیایت؟",
                    color = GoldenYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("confirm_action_button")
                    ) {
                        Text("جێبەجێی بکە", color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cancel_action_button")
                    ) {
                        Text("پەشیمان بوونەوە", fontSize = 12.sp)
                    }
                }
            }

            // Error & Settings Recovery Button
            if (message.status == ExecutionStatus.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("open_settings_recovery_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "کردنەوەی ڕێکخستن",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "کردنەوەی ڕێکخستنەکانی مۆبایل",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Extra Data Preview
            if (!message.resultMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isExpanded) "شاردنەوەی وردەکاری" else "پیشاندانی وردەکاری",
                        color = CyanGlow,
                        fontSize = 11.sp
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(SurfaceDark, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = message.resultMessage,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvidenceSourcesView(
    sources: List<GroundingSource>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark.copy(alpha = 0.85f))
            .border(0.5.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "سەرچاوەی بەڵگە",
                tint = GoldenYellow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "بەڵگە و سەرچاوە متمانەپێکراوەکان (${sources.size}):",
                color = GoldenYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            sources.take(6).forEach { source ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(0.5.dp, GoldenYellow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.uri)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = source.title.take(35),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "کردنەوەی سەرچاوە",
                        tint = GoldenYellow,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceLocationCard(
    location: PlaceLocation,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GoldenYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(YellowBadgeBg)
                        .border(1.dp, GoldenYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "شوێن",
                        tint = GoldenYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "لۆکەیشن و نەخشەی دەقیق",
                        color = GoldenYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = location.placeName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (location.cityAndCountry.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .border(0.5.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = location.cityAndCountry,
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Coordinates Badge with Copy Action
            if (location.coordinates.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, YellowBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📍 پۆوتان: ",
                            color = GoldenYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = location.coordinates,
                            color = GoldenYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Coordinates", location.coordinates)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "پۆوتانی جوگرافی کۆپیکرا: ${location.coordinates}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کۆپیکردنی پۆوتان",
                            tint = GoldenYellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Address
            if (location.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🏠 ناونیشان: ${location.address}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // Description
            if (location.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = location.description,
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Open in Google Maps
                Button(
                    onClick = {
                        val query = location.mapsQuery.ifBlank { location.placeName }
                        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            val webMapIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(webMapIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "نەخشەی گووگڵ",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Google Maps",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Directions
                OutlinedButton(
                    onClick = {
                        val dest = if (location.coordinates.isNotBlank()) location.coordinates else (location.mapsQuery.ifBlank { location.placeName })
                        val navIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}")
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        try {
                            context.startActivity(navIntent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "ڕێگا",
                        tint = CyanGlow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ڕێنمایی گەیشتن",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonProfileCard(
    person: RecognizedPerson,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131127)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PurpleGlow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(YellowBadgeBg)
                        .border(1.dp, GoldenYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "کەسایەتی",
                        tint = GoldenYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کەسایەتی ناسراو",
                        color = GoldenYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = person.fullName,
                        color = GoldenYellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (person.profession.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(YellowBadgeBg)
                            .border(0.5.dp, YellowBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = person.profession,
                            color = GoldenYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bio
            if (person.biography.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = person.biography,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // Social accounts chips
            if (person.accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🌐 ئەکاونت و تۆڕە کۆمەڵایەتییەکان:",
                    color = GoldenYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    person.accounts.forEach { acc ->
                        val (platformName, targetUrl) = resolveSocialUrl(person.fullName, acc)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .border(0.5.dp, GoldenYellow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "کردنەوەی بەستەر نەتوانرا", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$platformName: ${acc.handleOrUrl.take(20)}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "کردنەوە",
                                tint = GoldenYellow,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Web Search fallback button
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val searchIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=${Uri.encode(person.fullName)}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(searchIntent)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "گەڕان لە گووگڵ",
                    tint = GoldenYellow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "گەڕانی زیاتر لە Google دەربارەی ${person.fullName}",
                    color = GoldenYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun resolveSocialUrl(personName: String, account: SocialAccount): Pair<String, String> {
    val handle = account.handleOrUrl.trim()
    val clean = handle.removePrefix("@")
    val platform = account.platform.trim()

    val url = when {
        handle.startsWith("http://") || handle.startsWith("https://") -> handle
        platform.contains("Instagram", ignoreCase = true) -> "https://instagram.com/$clean"
        platform.contains("Twitter", ignoreCase = true) || platform.contains("X", ignoreCase = true) -> "https://x.com/$clean"
        platform.contains("Facebook", ignoreCase = true) -> "https://facebook.com/$clean"
        platform.contains("LinkedIn", ignoreCase = true) -> "https://linkedin.com/in/$clean"
        platform.contains("YouTube", ignoreCase = true) -> "https://youtube.com/@$clean"
        platform.contains("Wikipedia", ignoreCase = true) -> "https://en.wikipedia.org/wiki/${Uri.encode(handle)}"
        else -> "https://www.google.com/search?q=" + Uri.encode("$personName $platform $handle")
    }

    return platform to url
}

@Composable
fun FormattedKurdishMessageText(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    if (isUser) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }

    val annotatedText = remember(text) {
        buildAnnotatedString {
            val boldRegex = Regex("""\*\*(.*?)\*\*""")
            val importantPrefixes = listOf(
                "گرنگ:", "گرنگە:", "تێبینی:", "ئاگاداری:", "سەرنج:", "پۆوتان:",
                "ناونیشان:", "سەرچاوە:", "بەڵگە:", "فەرمان:", "ئامانج:", "شوێن:", "کەسایەتی:"
            )

            var lastIndex = 0
            val matches = boldRegex.findAll(text).toList()

            if (matches.isEmpty()) {
                val lines = text.split("\n")
                lines.forEachIndexed { idx, line ->
                    var matchedPrefix = false
                    for (prefix in importantPrefixes) {
                        if (line.trimStart().startsWith(prefix)) {
                            withStyle(SpanStyle(color = GoldenYellow, fontWeight = FontWeight.Bold)) {
                                append(line)
                            }
                            matchedPrefix = true
                            break
                        }
                    }
                    if (!matchedPrefix) {
                        if (line.trimStart().startsWith("•") || line.trimStart().startsWith("-")) {
                            val colonIndex = line.indexOf(":")
                            if (colonIndex in 1..35) {
                                withStyle(SpanStyle(color = GoldenYellow, fontWeight = FontWeight.Bold)) {
                                    append(line.substring(0, colonIndex + 1))
                                }
                                withStyle(SpanStyle(color = TextPrimary)) {
                                    append(line.substring(colonIndex + 1))
                                }
                                matchedPrefix = true
                            }
                        }
                    }
                    if (!matchedPrefix) {
                        withStyle(SpanStyle(color = TextPrimary)) {
                            append(line)
                        }
                    }
                    if (idx < lines.size - 1) {
                        append("\n")
                    }
                }
            } else {
                for (match in matches) {
                    val range = match.range
                    if (range.first > lastIndex) {
                        val normalSegment = text.substring(lastIndex, range.first)
                        withStyle(SpanStyle(color = TextPrimary)) {
                            append(normalSegment)
                        }
                    }
                    val boldContent = match.groupValues[1]
                    withStyle(SpanStyle(color = GoldenYellow, fontWeight = FontWeight.Bold)) {
                        append(boldContent)
                    }
                    lastIndex = range.last + 1
                }
                if (lastIndex < text.length) {
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(text.substring(lastIndex))
                    }
                }
            }
        }
    }

    Text(
        text = annotatedText,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

