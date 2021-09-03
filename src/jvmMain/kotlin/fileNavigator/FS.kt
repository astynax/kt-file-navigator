package fileNavigator

import kotlinx.coroutines.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import kotlin.io.path.isDirectory
import kotlin.io.path.name

inline class FolderPath(val value: Path)

interface FolderLike {
    val watchable: Boolean
    val path: FolderPath
    val subfolders: Iterable<FolderPath>
    val files: Iterable<Path>
    val size: Int
}

data class FSPreviewItem(
    val level: Int,
    val name: String,
    val key: FolderPath? = null
)

class FS(
    private val scope: CoroutineScope,
    private val root: Path,
    private val openFolder: suspend (CoroutineScope, Path) -> FolderLike,
    private val onChange: (Iterator<FSPreviewItem>) -> Unit
) {
    private val activeFolders = mutableMapOf<FolderPath, Pair<FolderLike, WatchKey?>>()

    private val watchService = FileSystems.getDefault().newWatchService()

    private suspend fun activate(key: FolderPath, notify: Boolean = true) {
        this.let { fs ->
            scope.launch(Dispatchers.IO) {
                val folder = openFolder(this, key.value)
                val watchKey = if (folder.watchable) {
                    key.value.register(watchService, ENTRY_CREATE, ENTRY_DELETE)
                } else null
                activeFolders[key] = folder to watchKey
                delay(1000)
                onChange(fs.preview())
            }
        }
    }

    private suspend fun deactivate(key: FolderPath) {
        activeFolders[key]?.second?.cancel()
        activeFolders.remove(key)
        onChange(preview())
    }

    private suspend fun update(key: FolderPath) = this.let { fs ->
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
                name = path.value.name,
                key = path,
            )
        )
        subfolders.map { path ->
            when (val activeChild = activeFolders[path]?.first) {
                null -> yield(
                    FSPreviewItem(
                        level = level + 1,
                        name = path.value.name,
                        key = path
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
                    name = it.name
                )
            )
        }
    }

    fun preview(): Iterator<FSPreviewItem> =
        activeFolders[FolderPath(root)]?.first?.preview(0)
            ?: iterator {}

    init {
        scope.launch(Dispatchers.IO) {
            activate(FolderPath(root), notify = false)
            while (scope.isActive) {
                val key = watchService.take()
                (key.watchable() as? Path)?.let { watchablePath ->
                    key.pollEvents()
                        .filter { it.kind() == ENTRY_DELETE }
                        .map { watchablePath.resolve(it.context() as Path) }
                        .filter { it.isDirectory() }
                        .forEach { update(FolderPath(it)) }
                    update(FolderPath(watchablePath))
                }
                if (!key.reset()) {
                    key.cancel()
                    break
                }
            }
        }
    }

    suspend fun toggle(key: FolderPath) {
        if (key in activeFolders)
            deactivate(key)
        else activate(key)
    }

    fun measure(): Int =
        activeFolders[FolderPath(root)]?.let { (folder, _) ->
            folder.subfolders
                .sumOf {
                    it.measureIf { path -> activeFolders[path]?.first }
                } + folder.files.count()
        } ?: 0
}

private fun FolderPath.measureIf(lookup: (FolderPath) -> FolderLike?): Int =
    1 + when (val folder = lookup(this)) {
        null -> 0
        else ->
            folder.subfolders.sumOf { it.measureIf(lookup) } +
                    folder.files.count()
    }

class FSFolder(override val path: FolderPath): FolderLike {
    override val watchable = true
    private var _subfolders: List<FolderPath>? = null
    private var _files: List<Path>? = null
    private var _size: Int? = null

    override val subfolders
        get() = _subfolders ?: emptyList()

    override val files
        get() = _files ?: emptyList()

    override val size
        get() = _size ?: 0

    companion object {
        suspend fun make(scope: CoroutineScope, path: FolderPath): FSFolder =
            FSFolder(path).also {
                scope.launch(Dispatchers.IO) {
                    it._size = Files.newDirectoryStream(path.value).count()

                    it._subfolders = Files.newDirectoryStream(
                        path.value, DirectoryStream.Filter { it.isDirectory() }
                    ).map { FolderPath(it) }
                        .sortedBy { it.value.name }

                    it._files = Files.newDirectoryStream(
                        path.value, DirectoryStream.Filter { !it.isDirectory() }
                    ).toList()
                        .sortedBy { it.name }
                }.join()
        }
    }
}

private fun <T> Iterator<Iterator<T>>.join(): Iterator<T> =
    this.let { iterators ->
        object : Iterator<T> {
            var current = iterator()

            override fun hasNext(): Boolean = current.hasNext() || let {
                var result = false
                while (iterators.hasNext()) {
                    current = iterators.next()
                    if (current.hasNext()) {
                        result = true
                        break
                    }
                }
                result
            }

            override fun next(): T = current.next()
        }
    }

private fun <T> singletonIf(condition: Boolean, block: () -> T): Iterator<T> =
    iterator { if (condition) block() }
