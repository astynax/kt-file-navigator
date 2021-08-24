import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fileNavigator.*
import java.nio.file.Path

@Composable
fun UI() = Row(
    modifier = Modifier.fillMaxSize()
) {
    val state = rememberBrowserState()

    TreeView(
        root = Folder(Path.of(".")),
        state = state
    )
    Preview(
        path = state.selected
    )
}

fun main() = Window(
    title = "File Navigator"
) {
    DesktopMaterialTheme {
        UI()
    }
}
