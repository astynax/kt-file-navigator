import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.singleWindowApplication
import fileNavigator.Folder
import fileNavigator.Preview
import fileNavigator.TreeView
import fileNavigator.rememberBrowserState
import java.nio.file.Path

@Composable
fun UI() = Row(
    modifier = Modifier.fillMaxSize()
) {
    val state = rememberBrowserState(
        root = Folder(Path.of("."))
    )

    TreeView(
        state = state,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
    )
    Preview(
        path = state.selected,
        modifier = Modifier
            .fillMaxHeight()
            .weight(3f)
    )
}

fun main() = singleWindowApplication(
    title = "File Navigator"
) {
    DesktopMaterialTheme {
        UI()
    }
}
