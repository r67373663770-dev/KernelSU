package me.weishu.kernelsu.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.theme.LocalEnableSwitchDrag

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = expressiveSwitchColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    showThumbIcon: Boolean = true,
) {
    if (LocalEnableSwitchDrag.current) {
        DraggableExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            interactionSource = interactionSource,
            showThumbIcon = showThumbIcon,
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent ?: if (showThumbIcon && (checked || enabled)) {
                {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource
        )
    }
}

// Metrics mirroring the M3 switch specification.
private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 32.dp
private val SwitchThumbSizeUnchecked = 16.dp
private val SwitchThumbSizeChecked = 24.dp
private val SwitchThumbInset = 6.dp
private val SwitchThumbLeftChecked =
    SwitchTrackWidth - SwitchThumbInset - SwitchThumbSizeChecked
private val SwitchTrackOutlineWidth = 2.dp

// A flick faster than this commits towards the flick direction instead of the
// position threshold. Expressed in dp so it scales with density.
private val SwitchFlickVelocity = 1000.dp

@Composable
private fun DraggableExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    showThumbIcon: Boolean = true,
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // travel = distance between unchecked-thumb-center and checked-thumb-center
    val thumbTravel = SwitchThumbLeftChecked + SwitchThumbSizeChecked / 2 -
        (SwitchThumbInset + SwitchThumbSizeUnchecked / 2)
    val travelPx = with(density) { thumbTravel.toPx() }
    val flickVelocityPx = with(density) { SwitchFlickVelocity.toPx() }

    val thumbAnimatable = remember { Animatable(if (checked) 1f else 0f) }
    var isDragging by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    LaunchedEffect(checked) {
        if (!isDragging) {
            thumbAnimatable.animateTo(if (checked) 1f else 0f, settleSpec)
        }
    }

    val position = if (enabled) thumbAnimatable.value else if (checked) 1f else 0f

    val colorScheme = MaterialTheme.colorScheme
    val trackColor: Color
    val thumbColor: Color
    val borderColor: Color
    val iconColor: Color
    if (!enabled) {
        if (checked) {
            thumbColor = colorScheme.surface.copy(alpha = 0.38f)
            trackColor = colorScheme.onSurface.copy(alpha = 0.12f)
            borderColor = Color.Transparent
            iconColor = colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            thumbColor = colorScheme.outline.copy(alpha = 0.38f)
            trackColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.12f)
            borderColor = colorScheme.onSurface.copy(alpha = 0.12f)
            iconColor = colorScheme.surfaceContainerHighest
        }
    } else {
        trackColor = lerpColor(colorScheme.surfaceContainerHighest, colorScheme.primary, position)
        thumbColor = lerpColor(colorScheme.outline, colorScheme.onPrimary, position)
        borderColor = colorScheme.outline.copy(alpha = 1f - position)
        iconColor = if (position > 0.5f) colorScheme.primary else colorScheme.surfaceContainerHighest
    }

    val thumbSize = lerpDp(SwitchThumbSizeUnchecked, SwitchThumbSizeChecked, position)
    val thumbLeft = lerpDp(SwitchThumbInset, SwitchThumbLeftChecked, position)
    val thumbLeftAbsolute = if (isRtl) {
        SwitchTrackWidth - thumbLeft - thumbSize
    } else {
        thumbLeft
    }
    val thumbTop = (SwitchTrackHeight - thumbSize) / 2

    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)

    val gestureModifier = if (!enabled || onCheckedChange == null) {
        Modifier
    } else {
        Modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = true,
                role = Role.Switch,
                onValueChange = { currentOnCheckedChange?.invoke(it) }
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        val vx = (if (isRtl) -1f else 1f) *
                            velocityTracker.calculateVelocity().x
                        velocityTracker.resetTracking()
                        val target = when {
                            vx > flickVelocityPx -> true
                            vx < -flickVelocityPx -> false
                            else -> thumbAnimatable.value > 0.5f
                        }
                        if (target != currentChecked) {
                            currentOnCheckedChange?.invoke(target)
                        } else {
                            scope.launch {
                                thumbAnimatable.animateTo(
                                    if (target) 1f else 0f, settleSpec
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        velocityTracker.resetTracking()
                        scope.launch {
                            thumbAnimatable.animateTo(
                                if (currentChecked) 1f else 0f, settleSpec
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        val delta = (if (isRtl) -dragAmount else dragAmount) / travelPx
                        val newPos = (thumbAnimatable.value + delta).coerceIn(0f, 1f)
                        val oldPos = thumbAnimatable.value
                        scope.launch { thumbAnimatable.snapTo(newPos) }
                        if ((oldPos > 0.5f) != (newPos > 0.5f)) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        }
                    }
                )
            }
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(gestureModifier)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(Modifier.size(SwitchTrackWidth, SwitchTrackHeight)) {
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                if (borderColor.alpha > 0f) {
                    val stroke = SwitchTrackOutlineWidth.toPx()
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius((size.height - stroke) / 2f),
                        style = Stroke(stroke)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = thumbLeftAbsolute, y = thumbTop)
                    .size(thumbSize)
                    .background(thumbColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (thumbContent != null) {
                    thumbContent()
                } else if (showThumbIcon && (position > 0.5f || enabled)) {
                    Icon(
                        imageVector = if (position > 0.5f) {
                            Icons.Filled.Check
                        } else {
                            Icons.Filled.Close
                        },
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = iconColor
                    )
                }
            }
        }
    }
}

@Composable
fun expressiveSwitchColors(
    checkedIconColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledCheckedIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
    disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f),
    disabledUncheckedBorderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
): SwitchColors = SwitchDefaults.colors(
    checkedIconColor = checkedIconColor,
    uncheckedIconColor = uncheckedIconColor,
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor,
    disabledCheckedIconColor = disabledCheckedIconColor,
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledUncheckedIconColor = disabledUncheckedIconColor,
)
