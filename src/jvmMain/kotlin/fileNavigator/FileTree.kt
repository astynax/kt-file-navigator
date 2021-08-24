package fileNavigator

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

abstract class FileTreeNode {
    abstract val path: Path

    abstract val hasChildren: Boolean
    abstract val children: List<FileTreeNode>

    val name: String
        get() = path.name
}

class Folder(override val path: Path): FileTreeNode() {
    override val hasChildren: Boolean = true

    private var cachedChildren: List<FileTreeNode>? = null

    override val children: List<FileTreeNode>
        get() {
            return cachedChildren
                ?: Files
                    .newDirectoryStream(path)
                    .map {
                            if (it.isDirectory())
                                Folder(it)
                            else File(it)
                        }
                    .also { cachedChildren = it }
        }
}

class File(override val path: Path): FileTreeNode() {
    override val hasChildren = false
    override val children = emptyList<FileTreeNode>()
}
