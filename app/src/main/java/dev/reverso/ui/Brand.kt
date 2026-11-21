package dev.reverso.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.reverso.R

@Composable
fun ReversoMark(size: Dp, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(size), color = ReversoColors.Raised, shape = RoundedCornerShape(size * 0.28f)) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.reverso_mark),
                contentDescription = "Reverso mark",
                modifier = Modifier.size(size * 0.78f),
            )
        }
    }
}
