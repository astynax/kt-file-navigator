package fileNavigator

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import java.nio.file.Path

class BrowserState(
    private val unfolded: Set<Path>,
    val selected: Path?,
    val toggle: (Path) -> Unit,
    val select: (Path) -> Unit,
    val items: List<ListItem>
) {
    fun isUnfolded(path: Path) = path in unfolded

    fun selectFirst() {
        items.firstOrNull()?.let { select(it.node.path) }
    }

    fun selectLast() {
        items.lastOrNull()?.let { select(it.node.path) }
    }

    fun selectPrevious() {
        when (selected) {
            null -> selectFirst()
            else -> items.asSequence().zipWithNext()
                .find { (_, next) ->
                    next.node.path == selected
                }?.let { (previous, _) ->
                    select(previous.node.path)
                }
        }
    }

    fun selectNext() {
        when (selected) {
            null -> selectFirst()
            else -> items.asSequence().zipWithNext()
                .find { (previous, _) ->
                    previous.node.path == selected
                }?.let { (_, next) ->
                    select(next.node.path)
                }
        }
    }
}

data class ListItem(
    val level: Int,
    val node: FileTreeNode
)

@Composable
fun rememberBrowserState(root: Folder): BrowserState {
    var unfolded by remember { mutableStateOf(emptySet<Path>()) }
    var selected by remember { mutableStateOf<Path?>(null) }
    var items by remember {
        mutableStateOf(
            root.listVisible(unfolded)
        )
    }

    return BrowserState(
        items = items,
        unfolded = unfolded,
        selected = selected,
        toggle = {
            unfolded = unfolded.toggle(it)
            items = root.listVisible(unfolded)
        },
        select = { selected = it }
    )
}

@Composable
fun TreeView(
    state: BrowserState,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
        .background(color = Color.White)
        .border(width = 1.dp, color = Color.Gray)
        .focusable()
        .onPreviewKeyEvent(state::navigateByKeys)
) {
    state.items.forEach {
        Row {
            Pad(it.level)
            if (it.node.hasChildren)
                FolderView(it.node, state)
            else FileView(it.node, state)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun BrowserState.navigateByKeys(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown && (mapOf(
        Key.MoveHome to { this.selectFirst() },
        Key.MoveEnd to { this.selectLast() },
        Key.DirectionUp to { this.selectPrevious() },
        Key.DirectionDown to { this.selectNext() }
    )[event.key]
        ?.let { it(); true }
        ?: false)

@Composable
fun Pad(level: Int) = Spacer(
    Modifier.size(
        width = (20 * (level - 1)).dp,
        // ^ root level "0" is invisible so no offset needed
        height = 0.dp
    )
)

@Composable
fun FolderView(
    node: FileTreeNode,
    state: BrowserState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .highlightIf(state.selected == node.path)
            .clickable { state.select(node.path) }
    ) {
        Icon(
            if (state.isUnfolded(node.path)) Icons.Rounded.ArrowDropDown
            else Icons.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.clickable { state.toggle(node.path) }
        )
        Text(node.name)
    }
}

@Composable
fun FileView(
    node: FileTreeNode,
    state: BrowserState,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .highlightIf(state.selected == node.path)
        .clickable { state.select(node.path) }
) {
    Icon(Icons.Rounded.Check, contentDescription = null)
    Text(node.name)
}

fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this)
        this - item
    else this + item

fun Modifier.highlightIf(condition: Boolean): Modifier =
    if (condition)
        this.background(color = Color.LightGray)
    else this

fun FileTreeNode.listVisible(visible: Set<Path>, level: Int = 0): List<ListItem> {
    val itself = if (level == 0)
        listOf()  // root node is always invisible
    else listOf(
        ListItem(level, this)
    )
    val nextOnes = if (
        this.hasChildren &&
        (level == 0  // root-level children are always visible
                || this.path in visible)
    ) {
        val folders =
            this.children.filter { it.hasChildren }
                .sortedBy { it.name }
        val files =
            this.children.filter { !it.hasChildren }
                .sortedBy { it.name }
        (folders + files)  // folders appear before files
            .flatMap { it.listVisible(visible, level + 1) }
    }
    else listOf()
    return itself + nextOnes
}
