package com.voiddevelopers.rotorydialer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voiddevelopers.rotorydialer.ui.theme.RotoryDialerTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

const val TAG = "Surendhar TAG"

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

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .size(300.dp)
                .background(Color.Black)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation.value)
            ) {

                drawLine(
                    start = Offset(size.width / 2, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    color = Color.Red,
                )

                drawCircle(
                    radius = 10.dp.toPx(),
                    color = Color.White,
                    center = Offset(size.width / 2, size.height / 2)
                )

                val radius = size.width / 2.7f
                val diameter = radius * 2

                val startAngle = 50f
                val sweepAngle = 260f

                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    size = Size(diameter, diameter),
                    style = Stroke(
                        width = 60.dp.toPx(),
                        cap = StrokeCap.Round
                    ),
                    topLeft = Offset(
                        x = (size.width - diameter) / 2,
                        y = (size.height - diameter) / 2
                    ),
                )

                val circleCount = 10

                for (i in 0 until circleCount) {

                    val angle = startAngle + (sweepAngle / (circleCount - 1)) * i
                    val angleRad = Math.toRadians(angle.toDouble())

                    val x = center.x + radius * kotlin.math.cos(angleRad).toFloat()
                    val y = center.y + radius * kotlin.math.sin(angleRad).toFloat()

                    drawCircle(
                        color = Color.Black,
                        radius = 24.dp.toPx(),
                        center = Offset(x, y)
                    )

                }

            }
        }

        var isDragAllowed = remember { true }
        var previousAngle: Float? = remember { null }
        var rotationAngle: Float = remember { 0f }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .size(300.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            scope.launch {
                                rotation.stop()
                            }

                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            val dx = offset.x - cx
                            val dy = offset.y - cy

                            previousAngle =
                                Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

                        },
                        onDrag = { change, _ ->

                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            val dx = change.position.x - cx
                            val dy = change.position.y - cy

                            val currentAngle =
                                Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

                            previousAngle?.let { prev ->
                                var delta = currentAngle - prev

                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f

                                rotationAngle += delta

                                scope.launch {
                                    rotation.snapTo(rotationAngle)
                                }
                            }

                            previousAngle = currentAngle
                            change.consume()
                        },
                        onDragEnd = {
                            previousAngle = null
                            isDragAllowed = true
                            scope.launch {
                                rotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 500)
                                )
                                rotationAngle = 0f
                            }
                        }
                    )
                }
        )

    }
}

fun getAngle(cx: Float, cy: Float, px: Float, py: Float): Float {
    val dx = px - cx
    val dy = py - cy

    var angle = atan2(dy, dx) * 180f / PI.toFloat()

    if (angle < 0) angle += 360f

    return angle
}

@Preview
@Composable
fun PreviewDialerScreen() {
    DialerScreen(modifier = Modifier)
}


