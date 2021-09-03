import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import fileNavigator.FS
import fileNavigator.FSPreviewItem
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
fun FSView() {
    val (data, updateData) = remember {
        mutableStateOf<List<FSPreviewItem>>(listOf())
    }
    val scope = rememberCoroutineScope()
    val fs = remember {
        FS(
            scope = scope,
            root = Path.of("."),
            onChange = updateData
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = data, key = { it.path }) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width((16 * it.level).dp))
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .then(when (val key = it.key) {
                            null -> Modifier
                            else -> Modifier.clickable {
                                scope.launch { fs.toggle(key) }
                            }
                        }),
                    text = it.name,
                    style = when (it.key) {
                        null -> TextStyle.Default
                        else -> TextStyle.Default.copy(fontWeight = FontWeight.Bold)
                    }
                )
            }
        }
    }
}

fun main() = singleWindowApplication(
    title = "File Navigator"
) {
    DesktopMaterialTheme {
        FSView()
    }
}
