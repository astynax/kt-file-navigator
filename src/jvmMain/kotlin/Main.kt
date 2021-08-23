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
import fileNavigator.FileTreeNode
import fileNavigator.Folder
import java.nio.file.Path

class TreeState(
    private val unfolded: Set<Path>,
    private val selected: Path?,
    val toggle: (Path) -> Unit,
    val select: (Path) -> Unit,
) {
    fun isUnfolded(path: Path) = path in unfolded
    fun isSelected(path: Path) = path == selected
}

fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this)
        this - item
    else this + item

fun Modifier.highlightIf(condition: Boolean): Modifier =
    if (condition)
        this.background(color = Color.LightGray)
            .border(1.dp, Color.Red)
    else this

@Composable
fun FolderItems(
    items: List<FileTreeNode>,
    state: TreeState,
    level: Int,
) {
    items.filter { it.hasChildren }
        .sortedBy { it.name }
        .forEach {
            FolderView(it, state, level)
        }
    items.filter { !it.hasChildren }
        .sortedBy { it.name }
        .forEach {
            FileView(it, state, level)
        }
    }

@Composable
fun Pad(level: Int) = Spacer(
    Modifier.size(width = (20 * level).dp, height = 0.dp)
)

@Composable
fun FolderView(
    node: FileTreeNode,
    state: TreeState,
    level: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset(x = level.dp)
            .fillMaxWidth()
            .highlightIf(state.isSelected(node.path))
            .clickable { state.select(node.path) }
    ) {
        Pad(level)
        Icon(
            if (state.isUnfolded(node.path)) Icons.Rounded.ArrowDropDown
            else Icons.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.clickable { state.toggle(node.path) }
        )
        Text(node.name)
    }
    if (state.isUnfolded(node.path)) {
        FolderItems(node.children, state, level + 1)
    }
}

@Composable
fun FileView(
    node: FileTreeNode,
    state: TreeState,
    level: Int,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .highlightIf(state.isSelected(node.path))
        .clickable { state.select(node.path) }
) {
    Pad(level)
    Icon(Icons.Rounded.Check, contentDescription = null)
    Text(node.name)
}

@Composable
fun TreeView(
    root: Folder,
    state: TreeState
) = Column(
    modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.3f)
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
) {
    FolderItems(root.children, state, level = 0)
}

@Composable
fun Preview(path: Path?) = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .fillMaxSize()
        .background(color = Color(180, 180, 180))
) {
    Text("${path ?: ""}")
}

@Composable
fun UI() = Row(
    modifier = Modifier.fillMaxSize()
) {
    var unfolded by remember { mutableStateOf(emptySet<Path>()) }
    var selected by remember { mutableStateOf<Path?>(null) }

    TreeView(
        Folder(Path.of(".")),
        TreeState(
            unfolded = unfolded,
            selected = selected,
            toggle = { unfolded = unfolded.toggle(it) },
            select = { selected = it }
        )
    )
    Preview(selected)
}

fun main() = Window(
    title = "File Navigator"
) {
    DesktopMaterialTheme {
        UI()
    }
}
