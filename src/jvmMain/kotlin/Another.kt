import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import fileNavigator.FSFolder
import fileNavigator.FSPreviewItem
import fileNavigator.FolderPath
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
fun FSView() {
    val verticalScrollState = rememberScrollState(0)
    val (itemFlow, updateFlow) = remember {
        mutableStateOf<Iterator<FSPreviewItem>>(iterator {})
    }
    val scope = rememberCoroutineScope()
    val fs = remember {
        FS(
            scope = scope,
            root = Path.of("."),
            onChange = updateFlow,
            openFolder = { scope, path ->
                FSFolder.make(scope, FolderPath(path))
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        itemFlow.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width((16 * item.level).dp))
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .then(when (item.key) {
                            null -> Modifier
                            else -> Modifier.clickable {
                                scope.launch { fs.toggle(item.key) }
                            }
                        }),
                    text = item.name,
                    style = when (item.key) {
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
