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

data class FSPreviewItem(
    val level: Int,
    val name: String,
    val path: Path,
    val key: FolderId? = null
)

class FS(
    private val scope: CoroutineScope,
    private val root: Path,
    private val onChange: (List<FSPreviewItem>) -> Unit,
    private val openFolder: suspend (CoroutineScope, Path) -> FolderLike =
        { outerScope, path -> FSFolder.make(outerScope, path) }
) {
    private val activeFolders = mutableMapOf<FolderId, Pair<FolderLike, WatchKey?>>()

    private val watchService = FileSystems.getDefault().newWatchService()

    private suspend fun activate(key: FolderId, delayed: Boolean = false) {
        this.let { fs ->
            scope.launch(Dispatchers.IO) {
                val folder = openFolder(this, key.value)
                val watchKey = if (folder.watchable) {
                    key.value.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )
                } else null
                activeFolders[key] = folder to watchKey
                if (delayed) delay(500)
                // TODO: need to bind to the lifetime of the view
                onChange(fs.preview())
            }
        }
    }

    private fun deactivate(key: FolderId) {
        activeFolders[key]?.second?.cancel()
        activeFolders.remove(key)
        onChange(preview())
    }

    private suspend fun update(key: FolderId) = this.let { fs ->
        activeFolders[key]?.second?.let { watchKey ->
            scope.launch(Dispatchers.IO) {
                activeFolders.replace(
                    key,
                    openFolder(this, key.value) to watchKey
                )
                onChange(fs.preview())
            }
        }
    }

    private fun FolderLike.preview(level: Int): Iterator<FSPreviewItem> = iterator {
        if (level != 0) yield(
            FSPreviewItem(
                level = level,
                name = path.name,
                path = path,
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
                    path = it
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
            activate(FolderId(root), delayed = true)
            while (scope.isActive) {
                val key = watchService.poll(100, TimeUnit.MILLISECONDS) ?: continue
                (key.watchable() as? Path)?.let { watchablePath ->
                    key.pollEvents()
                        .filter { it.kind() == StandardWatchEventKinds.ENTRY_DELETE }
                        .map { watchablePath.resolve(it.context() as Path) }
                        .filter { it.isDirectory() }
                        .forEach { update(FolderId(it)) }
                    update(FolderId(watchablePath))
                }
                if (!key.reset()) {
                    key.cancel()
                    break
                }
            }
        }
    }

    suspend fun toggle(key: FolderId) {
        if (key in activeFolders)
            deactivate(key)
        else activate(key)
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
