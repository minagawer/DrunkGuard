package com.minagawer.drunkguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.minagawer.drunkguard.ui.theme.BgBase
import com.minagawer.drunkguard.ui.theme.BgBorder
import com.minagawer.drunkguard.ui.theme.BgCard
import com.minagawer.drunkguard.ui.theme.BgCardHigh
import com.minagawer.drunkguard.ui.theme.DrunkGuardTheme
import com.minagawer.drunkguard.ui.theme.GreenActive
import com.minagawer.drunkguard.ui.theme.GreenDim
import com.minagawer.drunkguard.ui.theme.RedDanger
import com.minagawer.drunkguard.ui.theme.RedDim
import com.minagawer.drunkguard.ui.theme.TextPrimary
import com.minagawer.drunkguard.ui.theme.TextSecondary

private data class MathQuestion(
    val text: String,
    val answer: Int
)

private fun generateQuestion(): MathQuestion {
    val a = (10..30).random()
    val b = (3..9).random()
    val c = (10..30).random()
    val d = (3..9).random()
    return when ((0..3).random()) {
        0 -> MathQuestion("$a × $b = ?", a * b)
        1 -> MathQuestion("$a × $b + $c = ?", a * b + c)
        2 -> MathQuestion("$a × $b − $c = ?", a * b - c)
        else -> MathQuestion("($a + $b) × $d = ?", (a + b) * d)
    }
}

class SobrietyCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrunkGuardTheme {
                SobrietyCheckScreen(
                    onSuccess = { stopGuardAndFinish() },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun stopGuardAndFinish() {
        val prefs = getSharedPreferences(AppMonitorService.PREFS_NAME, MODE_PRIVATE)
        prefs.edit { putBoolean(AppMonitorService.KEY_GUARD_ACTIVE, false) }
        stopService(Intent(this, AppMonitorService::class.java))
        Toast.makeText(this, "ガードを解除しました！", Toast.LENGTH_LONG).show()
        finish()
    }
}

@Composable
fun SobrietyCheckScreen(onSuccess: () -> Unit, onCancel: () -> Unit) {
    val totalQuestions = 3

    var currentStep by remember { mutableIntStateOf(1) }
    var question by remember { mutableStateOf(generateQuestion()) }
    var userInput by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // シェイクアニメーション
    var shakeKey by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            repeat(4) { i ->
                shakeOffset.animateTo(
                    targetValue = if (i % 2 == 0) 14f else -14f,
                    animationSpec = tween(65, easing = LinearEasing)
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(65))
        }
    }

    // 🧠 バウンスアニメーション
    val brainTransition = rememberInfiniteTransition(label = "brainBounce")
    val brainOffsetY by brainTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brainOffsetY"
    )

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentStep) {
        question = generateQuestion()
        userInput = ""
        feedback = null
        isError = false
        focusRequester.requestFocus()
    }

    fun submit() {
        val answer = userInput.trim().toIntOrNull()
        if (answer == null) {
            feedback = "数字を入力してください"
            isError = true
            return
        }
        if (answer == question.answer) {
            if (currentStep >= totalQuestions) {
                keyboard?.hide()
                onSuccess()
            } else {
                feedback = "✅ 正解！"
                isError = false
                currentStep++
            }
        } else {
            isError = true
            feedback = "❌ 不正解。最初からやり直し"
            shakeKey++
            currentStep = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🧠",
                    fontSize = 32.sp,
                    modifier = Modifier.offset(y = brainOffsetY.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "素面チェック",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalQuestions 問全問正解でロック解除",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // ── Progress indicator ────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalQuestions) { index ->
                    val isCompleted = index < currentStep - 1 ||
                            (index == currentStep - 1 && feedback == "✅ 正解！")
                    val isCurrent = index == currentStep - 1 && feedback != "✅ 正解！"

                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isCompleted -> GreenActive
                            isCurrent -> GreenActive.copy(alpha = 0.4f)
                            else -> BgCardHigh
                        },
                        animationSpec = tween(300),
                        label = "dotColor$index"
                    )
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 32.dp else 10.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dotWidth$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Question card ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeOffset.value.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BgCard)
                    .border(1.dp, BgBorder, RoundedCornerShape(28.dp))
                    .padding(vertical = 40.dp, horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "問題  $currentStep / $totalQuestions",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedContent(
                        targetState = question,
                        transitionSpec = {
                            (slideInHorizontally { it / 2 } + fadeIn(tween(220))).togetherWith(
                                slideOutHorizontally { -it / 2 } + fadeOut(tween(180))
                            )
                        },
                        label = "questionTransition"
                    ) { q ->
                        Text(
                            text = q.text,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it.filter { c -> c.isDigit() || c == '-' } },
                        modifier = Modifier
                            .width(160.dp)
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenActive,
                            unfocusedBorderColor = BgBorder,
                            errorBorderColor = RedDanger,
                            focusedContainerColor = BgCardHigh,
                            unfocusedContainerColor = BgCardHigh,
                            errorContainerColor = RedDim,
                        ),
                        isError = isError,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = {
                            Text(
                                "答え",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                    )
                    AnimatedVisibility(
                        visible = feedback != null,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -8 }),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = feedback ?: "",
                                color = if (isError) RedDanger else GreenActive,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Buttons ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenActive,
                        disabledContainerColor = GreenDim
                    ),
                    enabled = userInput.isNotBlank()
                ) {
                    Text(
                        "回答する",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "諦める",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
