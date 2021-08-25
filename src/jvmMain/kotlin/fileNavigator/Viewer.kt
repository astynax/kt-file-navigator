package fileNavigator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

@Composable
fun Balloon(
    text: String,
    icon: ImageVector? = Icons.Default.Info,
    color: Color = Color.White
) = Box(
    modifier = Modifier.background(color = color)
) {
    Row(
        modifier = Modifier.padding(all = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Icon(it, contentDescription = null) }
        Text(text)
    }
}

@Composable
fun Preview(
    path: Path?,
    modifier: Modifier = Modifier
) = ScrollableBox(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .background(color = Color.LightGray)
        .border(width = 1.dp, color = Color.Gray)
        .focusable()
) { innerModifier ->
    var preview by remember { mutableStateOf<@Composable () -> Unit>({}) }

    LaunchedEffect(path) {
        when (path) {
            null -> preview = {}
            else -> this.runCatching {
                getPreviewer(
                    type = path.getTypeAsync(),
                    path = path
                )(path)
            }.fold(
                onSuccess = { preview = it },
                onFailure = {
                    when (it) {
                        is CancellationException -> {
                        }
                        else -> {
                            print(it.stackTraceToString())
                            preview = {
                                Balloon(
                                    "Unable to preview this file.",
                                    icon = Icons.Default.Warning,
                                    color = Color.Yellow
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = innerModifier
    ) {
        preview()
    }
}


private suspend fun getPreviewer(
    type: String?, path: Path
): suspend (Path) -> (@Composable () -> Unit) =
    type?.let {
        mapOf(
            // known MIME-types
            "text/markdown" to ::previewText,
        )[it]
    } ?: when {
        // path-based rules
        path.extension in setOf("kt", "kts") -> ::previewText
        path.name == ".gitignore" -> ::previewText
        else -> ::defaultPreview
    }

private suspend fun previewText(path: Path): @Composable () -> Unit {
    path.readTextAsync().let {
        return { Box(
            modifier = Modifier
                .background(color = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .padding(all = 5.dp)
            ) {
                Text(it)
            }
        }}
    }
}

private suspend fun defaultPreview(path: Path): @Composable () -> Unit =
    { Balloon(path.name) }

private suspend fun Path.getTypeAsync(): String? = this.let { path ->
    withContext(Dispatchers.IO) {
        Files.probeContentType(path)
    }
}

private suspend fun Path.readTextAsync(): String = this.let { path ->
    withContext(Dispatchers.IO) {
        path.toFile().readText()
    }
}
