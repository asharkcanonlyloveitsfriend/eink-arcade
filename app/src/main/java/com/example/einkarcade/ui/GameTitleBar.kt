@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameTitleBar(
    setName: String,
    levelName: String,
    onOpenSetPicker: () -> Unit,
    onOpenLevelPicker: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = GAME_TITLE_TOP_PADDING_DP.dp,
                    bottom = GAME_TITLE_BOTTOM_PADDING_DP.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = levelName,
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier =
                    Modifier
                        .clickable { onOpenLevelPicker() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = setName,
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier =
                    Modifier
                        .clickable { onOpenSetPicker() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
