package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.VideoSource

internal data class ServerOption(
    val sourceIndex: Int,
    val label: String,
    val isSelected: Boolean
)

internal object PlayerServerMenuModel {

    fun isDub(source: VideoSource): Boolean {
        return listOf(source.language, source.server, source.quality).any {
            it.contains("dub", ignoreCase = true)
        }
    }

    fun preferredSourceIndex(
        sources: List<VideoSource>,
        currentSourceIndex: Int,
        showDub: Boolean
    ): Int {
        val currentIndex = sources.getOrNull(currentSourceIndex)
        if (currentIndex != null && isDub(currentIndex) == showDub) return currentSourceIndex
        return sources.indexOfFirst { isDub(it) == showDub }
    }

    fun controlLabel(
        sources: List<VideoSource>,
        selectedIndex: Int
    ): String {
        return sources.getOrNull(selectedIndex)?.let { source ->
            source.displayName.ifBlank { source.server.ifBlank { source.quality.ifBlank { "Source" } } }
        } ?: "Source"
    }

    fun options(
        sources: List<VideoSource>,
        selectedIndex: Int,
        showDub: Boolean
    ): List<ServerOption> {
        val counters = linkedMapOf<String, Int>()

        return sources.mapIndexedNotNull { index, source ->
            if (isDub(source) != showDub) return@mapIndexedNotNull null
            val baseName = source.displayName.ifBlank {
                source.server.ifBlank { source.quality.ifBlank { "Source" } }
            }
            val number = counters.merge(baseName, 1, Int::plus)
            ServerOption(
                sourceIndex = index,
                label = "$baseName $number",
                isSelected = index == selectedIndex
            )
        }
    }
}
