package msr.atsulab.app.player.domain.model

data class SkipInterval(
    val startMs: Long,
    val endMs: Long,
    val type: String = DEFAULT_TYPE
) {
    val isEnding: Boolean
        get() = type.equals(ENDING_TYPE, ignoreCase = true) ||
            type.equals(ENDING_LONG_TYPE, ignoreCase = true)

    companion object {
        const val DEFAULT_TYPE = "op"
        private const val ENDING_TYPE = "ed"
        private const val ENDING_LONG_TYPE = "ending"
    }
}
