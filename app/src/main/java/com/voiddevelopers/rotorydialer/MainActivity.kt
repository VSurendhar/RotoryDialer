package com.voiddevelopers.rotorydialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiddevelopers.rotorydialer.ui.theme.RotoryDialerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2

const val CIRCLE_COUNT = 10
const val ARC_START_ANGLE = 50f
const val ARC_SWEEP_ANGLE = 260f
val ANGLE_GAP = ARC_SWEEP_ANGLE / (CIRCLE_COUNT - 1)
val HALF_GAP = ANGLE_GAP / 2f

const val REFERENCE_ANGLE = 0f

// Digit labels in anti-clockwise order matching the dial layout
val DIGITS = listOf(0, 9, 8, 7, 6, 5, 4, 3, 2, 1)

data class Digit(var digit: Int, var color: Color)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RotoryDialerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DialerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DialerScreen(modifier: Modifier = Modifier) {

    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var dialedDigit by remember { mutableIntStateOf(-1) }
    val passwordList = remember { List(4, init = { mutableStateOf(Digit(-1, Color.White)) }) }
    var previousAngle: Float? = remember { null }
    var rotationAngle: Float = remember { 0f }

    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = "ENTER\nPASSCODE",
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            style = TextStyle(
                color = Color.Black,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Row(modifier = Modifier.fillMaxWidth()) {

            fun allFilled() {
                scope.launch {
                    for (i in 0..passwordList.size - 1) {
                        if (passwordList[i].value.digit == -1) return@launch
                        passwordList[i].value = passwordList[i].value.copy(color = Color.Green)
                        delay(50)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            repeat(4) { idx ->
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                ) {

                    val isFilled by remember { derivedStateOf { passwordList[idx].value.digit != -1 } }
                    val isAllFilled = passwordList.all { it.value.digit != -1 }

                    if (isAllFilled) allFilled()

                    val scaleValue by animateFloatAsState(
                        targetValue = if (isFilled) 1f else 0f,
                        animationSpec = spring()
                    )

                    Box(
                        modifier = Modifier
                            .scale(scaleValue)
                            .padding(4.dp)
                            .size(25.dp)
                            .clip(CircleShape)
                            .background(passwordList[idx].value.color)
                    )

                }
            }

            Spacer(modifier = Modifier.width(32.dp))

        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .size(300.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { offset ->
                        scope.launch {
                            rotation.stop()
                        }

                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        val dx = offset.x - cx
                        val dy = offset.y - cy

                        previousAngle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

                    }, onDrag = { change, _ ->

                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        val dx = change.position.x - cx
                        val dy = change.position.y - cy

                        val currentAngle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

                        previousAngle?.let { prev ->
                            var delta = currentAngle - prev

                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            if (rotationAngle + delta <= 310f) rotationAngle += delta

                            scope.launch {
                                rotation.snapTo(rotationAngle)
                            }
                        }

                        previousAngle = currentAngle
                        change.consume()
                    }, onDragEnd = {
                        previousAngle = null

                        // Capture the digit at the moment of release
                        val digitAtRelease = detectDigitAtReference(rotationAngle)
                        if (digitAtRelease != -1) {
                            dialedDigit = digitAtRelease
                            val idx =
                                passwordList.map { it.value.digit }.indexOfFirst { it == -1 }
                            if (idx != -1) {
                                passwordList[idx].value =
                                    passwordList[idx].value.copy(digit = dialedDigit)
                            }
                        }

                        scope.launch {
                            rotation.animateTo(
                                targetValue = 0f, animationSpec = tween(durationMillis = 500)
                            )
                            rotationAngle = 0f
                        }
                    })
                }) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation.value)
            ) {

                val radius = size.width / 2.7f
                val diameter = radius * 2

                drawArc(
                    color = Color.White,
                    startAngle = ARC_START_ANGLE,
                    sweepAngle = ARC_SWEEP_ANGLE,
                    useCenter = false,
                    size = Size(diameter, diameter),
                    style = Stroke(
                        width = 60.dp.toPx(), cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        x = (size.width - diameter) / 2, y = (size.height - diameter) / 2
                    ),
                )


                for (i in 0 until CIRCLE_COUNT) {

                    val angle = ARC_START_ANGLE + ANGLE_GAP * i
                    val angleRad = Math.toRadians(angle.toDouble())

                    val x = center.x + radius * kotlin.math.cos(angleRad).toFloat()
                    val y = center.y + radius * kotlin.math.sin(angleRad).toFloat()

                    drawCircle(
                        color = Color.Black, radius = 24.dp.toPx(), center = Offset(x, y)
                    )

                }

            }

            // Static digit labels (do not rotate)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val radius = size.width / 2.7f

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 20.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                for (i in 0 until CIRCLE_COUNT) {
                    val angle = ARC_START_ANGLE + ANGLE_GAP * i
                    val angleRad = Math.toRadians(angle.toDouble())

                    val x = center.x + radius * kotlin.math.cos(angleRad).toFloat()
                    val y = center.y + radius * kotlin.math.sin(angleRad).toFloat()

                    val textY = y - (textPaint.ascent() + textPaint.descent()) / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        DIGITS[i].toString(), x, textY, textPaint
                    )
                }
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val radius = size.width / 2.7f

                val angle = 0
                val angleRad = Math.toRadians(angle.toDouble())

                val x = center.x + radius * kotlin.math.cos(angleRad).toFloat()
                val y = center.y + radius * kotlin.math.sin(angleRad).toFloat()

                drawCircle(
                    color = Color.White, radius = 16.dp.toPx(), center = Offset(x, y)
                )

            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Made by Suren with ❤\uFE0F",
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.End,
            style = TextStyle(
                color = Color.LightGray,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = Modifier.height(45.dp))

    }
}


fun detectDigitAtReference(currentRotation: Float): Int {
    for (i in 0 until CIRCLE_COUNT) {
        val circleBaseAngle = ARC_START_ANGLE + ANGLE_GAP * i

        val effectiveAngle = circleBaseAngle + currentRotation

        var diff = effectiveAngle - REFERENCE_ANGLE

        // Normalize diff to [-180, 180]
        diff = ((diff % 360f) + 540f) % 360f - 180f

        if (diff >= -HALF_GAP && diff <= HALF_GAP) {
            return DIGITS[i]
        }
    }
    return -1
}


//@Preview
//@Composable
//fun PreviewDialerScreen() {
//    DialerScreen(modifier = Modifier)
//}
