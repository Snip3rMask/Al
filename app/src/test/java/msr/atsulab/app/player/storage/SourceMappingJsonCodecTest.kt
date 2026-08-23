package msr.atsulab.app.player.storage

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SourceMappingJsonCodecTest {

    private val codec = SourceMappingJsonCodec(Gson())

    @Test
    fun `mapping round trips provider picks and skipped providers`() {
        val mapping = SourceMapping(
            aniListId = " 21 ",
            picks = linkedMapOf(
                "Daki" to SourcePick(id = "daki-id", title = "Daki", thumbnailUrl = "https://example.test/thumb.jpg"),
                "Nora" to SourcePick(id = "nora-id")
            ),
            skipped = setOf("Hina", ""),
            confirmedAt = 123L
        )

        assertEquals(mapping.copy(aniListId = "21", skipped = setOf("Hina")), codec.decode(codec.encode(mapping), "21"))
    }

    @Test
    fun `incomplete json decodes as a safe empty mapping`() {
        val decoded = codec.decode("""{"confirmedAt":456}""", "42")

        assertEquals(SourceMapping(aniListId = "42", confirmedAt = 456L), decoded)
    }

    @Test
    fun `malformed and blank storage values do not resolve`() {
        assertNull(codec.decode("{invalid", "42"))
        assertNull(codec.decode(null, "42"))
        assertNull(codec.decode("""{}""", " "))
    }

    @Test
    fun `incoming mapping merges over existing mapping`() {
        val existing = SourceMapping(
            aniListId = "42",
            picks = mapOf("Daki" to SourcePick(id = "old")),
            skipped = setOf("Hina")
        )
        val incoming = SourceMapping(
            aniListId = " 42 ",
            picks = mapOf("Daki" to SourcePick(id = "new"), "Nora" to SourcePick(id = "nora")),
            skipped = setOf("Zoro")
        )

        val merged = incoming.mergedWith(existing, confirmedAt = 789L)

        assertEquals(
            SourceMapping(
                aniListId = "42",
                picks = mapOf(
                    "Daki" to SourcePick(id = "new"),
                    "Nora" to SourcePick(id = "nora")
                ),
                skipped = setOf("Hina", "Zoro"),
                confirmedAt = 789L
            ),
            merged
        )
    }
}
