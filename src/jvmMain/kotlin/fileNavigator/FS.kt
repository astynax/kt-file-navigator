package fileNavigator

import kotlinx.coroutines.*
import java.nio.file.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

inline class FolderId(val value: Path)

interface FolderLike {
    val path: Path
    val watchable: Boolean
    val subfolders: Iterable<Path>
    val files: Iterable<Path>
    val size: Int
}

enum class PreviewItemState {
    File,
    OpenedFolder,
    ClosedFolder
}

data class FSPreviewItem (
    val level: Int,
    val name: String,
    val path: Path,
    val state: PreviewItemState,
    val key: FolderId? = null
) {
    override fun equals(other: Any?): Boolean =
        this.path == (other as? FSPreviewItem)?.path

    override fun hashCode(): Int = path.hashCode()
}

class FS(
    private val scope: CoroutineScope,
    private val root: Path,
    private val onChange: (List<FSPreviewItem>) -> Unit,
    private val openFolder: suspend (CoroutineScope, Path) -> FolderLike =
        { outerScope, path -> FSFolder.make(outerScope, path) }
) {
    private val activeFolders = mutableMapOf<FolderId, Pair<FolderLike, WatchKey?>>()

    private val watchService = FileSystems.getDefault().newWatchService()

    private suspend fun activate(key: FolderId) {
        this.let { fs ->
            scope.launch(Dispatchers.IO) {
                if (key.value.exists()) {
                    val folder = openFolder(this, key.value)
                    val watchKey = if (folder.watchable) {
                        key.value.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE
                        )
                    } else null
                    activeFolders[key] = folder to watchKey
                }
            }
        }
    }

    private fun deactivate(key: FolderId) {
        activeFolders[key]?.second?.cancel()
        activeFolders.remove(key)
    }

    private suspend fun update(key: FolderId) = this.let { fs ->
        activeFolders[key]?.second?.let { watchKey ->
            scope.launch(Dispatchers.IO) {
                activeFolders.replace(
                    key,
                    openFolder(this, key.value) to watchKey
                )
            }
        }
    }

    private fun FolderLike.preview(level: Int): Iterator<FSPreviewItem> = iterator {
        if (level != 0) yield(
            FSPreviewItem(
                level = level,
                name = path.name,
                path = path,
                state = PreviewItemState.OpenedFolder,
                key = FolderId(path)
            )
        )
        subfolders.map { path ->
            when (val activeChild = activeFolders[FolderId(path)]?.first) {
                null -> yield(
                    FSPreviewItem(
                        level = level + 1,
                        name = path.name,
                        path = path,
                        state = PreviewItemState.ClosedFolder,
                        key = FolderId(path)
                    )
                )
                else -> yieldAll(
                    activeChild.preview(level + 1)
                )
            }
        }
        files.forEach {
            yield(
                FSPreviewItem(
                    level = level + 1,
                    name = it.name,
                    path = it,
                    state = PreviewItemState.File
                )
            )
        }
    }

    private fun preview(): List<FSPreviewItem> =
        activeFolders[FolderId(root)]?.first?.preview(0)
            ?.asSequence()?.toList()
            ?: emptyList()

    init {
        scope.launch(Dispatchers.IO) {
            activate(FolderId(root))
            delay(500)  // TODO: bind to the view's lifetime
            onChange(preview())
            while (scope.isActive) {
                delay(100)
                val deleted = mutableSetOf<Path>()
                val touched = mutableSetOf<Path>()
                while (true) {
                    val key = watchService.poll() ?: break
                    (key.watchable() as? Path)?.let { watchablePath ->
                        key.pollEvents().forEach {
                            if (it.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                val path = watchablePath.resolve(it.context() as Path)
                                if (FolderId(path) in activeFolders)
                                    deleted.add(path)
                            }
                        }
                        touched.add(watchablePath)
                    }
                    if (!key.reset()) {
                        // TODO: need to think about such cases
                        key.cancel()
                    }
                }
                touched.removeAll(deleted)
                deleted.forEach { deactivate(FolderId(it)) }
                touched.forEach {
                    if (it.exists()) update(FolderId(it))
                    else deactivate(FolderId(it))
                }
                onChange(preview())
            }
        }
    }

    suspend fun toggle(item: FSPreviewItem) {
        item.key?.also {
            if (it in activeFolders)
                deactivate(it)
            else activate(it)
            onChange(preview())
        }
    }
}

class FSFolder(override val path: Path): FolderLike {
    override val watchable = true

    private var _subfolders: List<Path>? = null
    override val subfolders get() = _subfolders ?: emptyList()

    private var _files: List<Path>? = null
    override val files get() = _files ?: emptyList()

    private var _size: Int? = null
    override val size get() = _size ?: 0

    companion object {
        suspend fun make(scope: CoroutineScope, path: Path): FSFolder =
            FSFolder(path).also {
                scope.launch(Dispatchers.IO) {
                    it._size = Files.newDirectoryStream(path).count()

                    it._subfolders = Files.newDirectoryStream(
                        path, DirectoryStream.Filter { it.isDirectory() }
                    ).sortedBy { it.name }

                    it._files = Files.newDirectoryStream(
                        path, DirectoryStream.Filter { !it.isDirectory() }
                    ).sortedBy { it.name }
                }.join()
        }
    }
}
