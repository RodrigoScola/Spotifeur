package fr.spotifeur.data

import fr.spotifeur.data.youtube.parseAutomix
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeAutomixTest {
    private fun panel(id: String, title: String, artist: String, length: String) = """
        {"playlistPanelVideoRenderer": {"videoId": "$id",
          "title": {"runs": [{"text": "$title"}]},
          "shortBylineText": {"runs": [{"text": "$artist"}]},
          "lengthText": {"runs": [{"text": "$length"}]}}}"""

    @Test fun readsTheQueuePanelInOrder() {
        val response = JSONObject("""{"contents": {"tabs": [{"content": {"musicQueueRenderer": {"content": {
            "playlistPanelRenderer": {"contents": [${panel("a1", "One", "Artist A", "3:05")}, {"automixPreviewVideoRenderer": {}},
            ${panel("b2", "Two", "Artist B", "1:02:03")}]}}}}}]}}""")
        val tracks = parseAutomix(response)
        assertEquals(listOf("ytm:track:a1", "ytm:track:b2"), tracks.map { it.id })
        assertEquals(listOf("One", "Two"), tracks.map { it.title })
        assertEquals(listOf("Artist A", "Artist B"), tracks.map { it.subtitle })
        assertEquals(listOf(185_000, 3_723_000), tracks.map { it.durationMs })
    }

    @Test fun emptyResponseHasNoTracks() {
        assertTrue(parseAutomix(JSONObject("{}")).isEmpty())
    }
}
