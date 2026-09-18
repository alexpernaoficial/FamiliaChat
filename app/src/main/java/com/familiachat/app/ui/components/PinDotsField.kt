package com.familiachat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PinDotsField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isError: Boolean = false,
    autoFocus: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val dotColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= length && new.all { it.isDigit() }) onValueChange(new)
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(length) { index ->
                    val filled = index < value.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(2.dp, dotColor, CircleShape)
                            .then(
                                if (filled) Modifier.background(dotColor, CircleShape) else Modifier
                            )
                    )
                }
            }
        }
    )

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}
