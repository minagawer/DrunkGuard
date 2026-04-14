package com.minagawer.drunkguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minagawer.drunkguard.ui.theme.BgCard
import com.minagawer.drunkguard.ui.theme.DrunkGuardTheme
import com.minagawer.drunkguard.ui.theme.RedBorder
import com.minagawer.drunkguard.ui.theme.TextSecondary

class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // バックキーを無効化（ガード解除まで戻れない）
        onBackPressedDispatcher.addCallback(this) { /* 何もしない */ }

        setContent {
            DrunkGuardTheme {
                BlockScreen(onGoHome = ::goHome)
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }
}

@Composable
fun BlockScreen(onGoHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF160306), Color(0xFF070001))
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Icon ──────────────────────────────────────────────────────────
            Text(text = "🔒", fontSize = 72.sp)

            // ── Title ─────────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LINE ロック中",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "飲酒ガードが有効です",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            // ── Info card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A0707))
                    .border(1.dp, RedBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "LINE は現在ロックされています。\n\nDrunkGuard アプリを開き、計算問題に\n全問正解するとロックを解除できます。",
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Home button ───────────────────────────────────────────────────────
        Button(
            onClick = onGoHome,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BgCard)
        ) {
            Text(
                text = "ホームに戻る",
                fontSize = 15.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}
