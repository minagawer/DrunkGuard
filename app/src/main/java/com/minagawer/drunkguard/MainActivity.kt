package com.minagawer.drunkguard

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.minagawer.drunkguard.ui.theme.*

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 結果は無視: 通知は補助的な機能 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            DrunkGuardTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var guardActive by remember {
        mutableStateOf(prefs.getBoolean(AppMonitorService.KEY_GUARD_ACTIVE, false))
    }
    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // onResume 相当: 画面が表示されるたびに権限状態を再チェック
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                guardActive = prefs.getBoolean(AppMonitorService.KEY_GUARD_ACTIVE, false)
                hasUsagePermission = hasUsageStatsPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allPermissionsGranted = hasUsagePermission && hasOverlayPermission

    val bgColor by animateColorAsState(
        targetValue = if (guardActive) Color(0xFF050F07) else BgBase,
        animationSpec = tween(800),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(36.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🍺", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DrunkGuard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "飲む前にLINEをロック",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // ── Status card ───────────────────────────────────────────────────
            StatusCard(guardActive = guardActive, allPermissionsGranted = allPermissionsGranted)

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom controls ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!hasUsagePermission) {
                    PermissionWarningCard(
                        text = "使用状況へのアクセス権限が必要"
                    ) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }

                if (!hasOverlayPermission) {
                    PermissionWarningCard(
                        text = "他のアプリへの表示権限が必要"
                    ) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }

                if (!guardActive && allPermissionsGranted) {
                    Button(
                        onClick = {
                            prefs.edit { putBoolean(AppMonitorService.KEY_GUARD_ACTIVE, true) }
                            context.startForegroundService(
                                Intent(context, AppMonitorService::class.java)
                            )
                            guardActive = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenActive)
                    ) {
                        Text(
                            text = "ガードを開始する",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else if (guardActive) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(context, SobrietyCheckActivity::class.java)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                    ) {
                        Text(
                            text = "ガードを解除する",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(guardActive: Boolean, allPermissionsGranted: Boolean = true) {
    val cardBg by animateColorAsState(
        targetValue = if (guardActive) GreenDim else BgCard,
        animationSpec = tween(600), label = "cardBg"
    )
    val borderBase by animateColorAsState(
        targetValue = if (guardActive) GreenBorder else BgBorder,
        animationSpec = tween(600), label = "borderBase"
    )
    val statusColor by animateColorAsState(
        targetValue = if (guardActive) GreenActive else TextSecondary,
        animationSpec = tween(600), label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val effectiveAlpha = if (guardActive) pulseAlpha else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                color = borderBase.copy(alpha = effectiveAlpha),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(vertical = 44.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (guardActive) "🛡️" else "🌙",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (guardActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = effectiveAlpha))
                    )
                    Text(
                        text = "ガード中",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Text(
                    text = "待機中",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    guardActive -> "LINEへのアクセスをブロックしています"
                    allPermissionsGranted -> "下のボタンを押してガードを開始"
                    else -> "権限を設定してからガードを開始できます"
                },
                fontSize = 13.sp,
                color = Color(0xFF9090B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PermissionWarningCard(text: String, onClickSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrangeDim)
            .border(1.dp, OrangeBorder, RoundedCornerShape(14.dp))
            .clickable { onClickSettings() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠\uFE0F",
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = text,
            color = OrangeWarn,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "›",
            color = OrangeWarn.copy(alpha = 0.85f),
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
