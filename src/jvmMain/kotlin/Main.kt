import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.singleWindowApplication
import fileNavigator.*
import java.nio.file.Path

@Composable
fun UI() = Row(
    modifier = Modifier.fillMaxSize()
) {
    val requester = remember { FocusRequester() }
    val selected = mutableStateOf<Path?>(null)
    val browserState = rememberBrowserState(
        root = Folder(Path.of(".")),
        selected = selected
    )
    val viewerState = rememberViewerState(
        selected = selected
    )

    Browser(
        state = browserState,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .focusRequester(requester)
    )
    Viewer(
        state = viewerState,
        modifier = Modifier
            .fillMaxHeight()
            .weight(3f)
    )

    LaunchedEffect(true) {
        requester.requestFocus()
    }
}

fun main() = singleWindowApplication(
    title = "File Navigator"
) {
    DesktopMaterialTheme {
        UI()
    }
}
