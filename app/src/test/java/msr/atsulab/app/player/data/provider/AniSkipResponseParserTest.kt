package msr.atsulab.app.player.data.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import msr.atsulab.app.player.domain.model.SkipInterval

class AniSkipResponseParserTest {

    @Test
    fun `resolves anilist media mal id`() {
        val json = """{"data":{"Media":{"idMal":987,"title":"ignored"}}}"""

        assertEquals(987, AniSkipResponseParser.resolveMalId(json))
        assertEquals(0, AniSkipResponseParser.resolveMalId("""{"errors":[]}"""))
    }

    @Test
    fun `parses valid op and ending intervals with millisecond rounding`() {
        val json = """
            {"found":true,"results":[
              {"skipType":"op","interval":{"startTime":10.5,"endTime":120}},
              {"skipType":"ed","interval":{"startTime":1300,"endTime":1380.25}},
              {"skipType":"bad","interval":{"startTime":-1,"endTime":20}},
              {"skipType":"bad","interval":{"startTime":30,"endTime":20}},
              {"skipType":"missing-interval"}
            ]}
        """.trimIndent()

        val intervals = AniSkipResponseParser.parseIntervals(json)

        assertEquals(2, intervals.size)
        assertEquals(10500L to 120000L, intervals[0].startMs to intervals[0].endMs)
        assertEquals("op", intervals[0].type)
        assertEquals(1300000L to 1380250L, intervals[1].startMs to intervals[1].endMs)
        assertEquals("ed", intervals[1].type)
    }

    @Test
    fun `returns empty intervals when not found or malformed`() {
        assertEquals(emptyList<SkipInterval>(), AniSkipResponseParser.parseIntervals("""{"found":false}"""))
        assertEquals(emptyList<SkipInterval>(), AniSkipResponseParser.parseIntervals("{"))
    }
}
