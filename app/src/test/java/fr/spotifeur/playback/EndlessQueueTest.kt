package fr.spotifeur.playback

import androidx.media3.common.Player
import fr.spotifeur.data.Music
import fr.spotifeur.data.MusicKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessQueueTest {
    private fun track(id: String) = Music(id, id, "a", MusicKind.Track)
    private fun spotify(n: Int) = track("spotify:track:$n")

    @Test fun extendsOnlyNearTheEndOfAFullyLoadedContext() {
        assertTrue(EndlessQueue.shouldExtend(remaining = 2, contextLeft = 0, repeatMode = Player.REPEAT_MODE_OFF))
        assertFalse(EndlessQueue.shouldExtend(remaining = 3, contextLeft = 0, repeatMode = Player.REPEAT_MODE_OFF))
        assertFalse(EndlessQueue.shouldExtend(remaining = 0, contextLeft = 40, repeatMode = Player.REPEAT_MODE_OFF))
        assertFalse(EndlessQueue.shouldExtend(remaining = 0, contextLeft = 0, repeatMode = Player.REPEAT_MODE_ALL))
        assertFalse(EndlessQueue.shouldExtend(remaining = 0, contextLeft = 0, repeatMode = Player.REPEAT_MODE_ONE))
    }

    @Test fun seedIsTheLastTrackWithSimilarTracks() {
        val queue = listOf(track("ytm:track:x"), spotify(1), spotify(2), track("local:3"))
        assertEquals("spotify:track:2", EndlessQueue.seed(queue, spotify = true)?.id)
        assertEquals("ytm:track:x", EndlessQueue.seed(queue, spotify = false)?.id)
        assertNull(EndlessQueue.seed(listOf(spotify(1), track("local:3")), spotify = false))
        assertNull(EndlessQueue.seed(emptyList(), spotify = true))
    }

    @Test fun pickSkipsQueuedAndRecentTracks() {
        val candidates = (1..6).map(::spotify)
        val excluded = setOf("spotify:track:1", "spotify:track:4")
        assertEquals(listOf(2, 3, 5, 6).map { "spotify:track:$it" }, EndlessQueue.pick(candidates, excluded).map(Music::id))
    }

    @Test fun pickKeepsEachTrackOnceAndOnlyTracks() {
        val candidates = listOf(spotify(1), spotify(1), track("ytm:track:x"), spotify(2), track("spotify:episode:e"), track("ytm:track:x"))
        assertEquals(listOf("spotify:track:1", "ytm:track:x", "spotify:track:2"), EndlessQueue.pick(candidates, emptySet()).map(Music::id))
    }

    @Test fun pickStopsAtTheLimit() {
        assertEquals(EndlessQueue.BATCH, EndlessQueue.pick((1..50).map(::spotify), emptySet()).size)
        assertEquals(2, EndlessQueue.pick((1..50).map(::spotify), emptySet(), limit = 2).size)
    }

    @Test fun nothingNewMeansNothingToAdd() {
        val candidates = listOf(spotify(1), spotify(2))
        assertTrue(EndlessQueue.pick(candidates, candidates.mapTo(HashSet(), Music::id)).isEmpty())
    }
}
