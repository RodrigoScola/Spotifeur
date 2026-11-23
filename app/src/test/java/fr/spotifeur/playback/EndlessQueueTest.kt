package fr.spotifeur.playback

import fr.spotifeur.data.Music
import fr.spotifeur.data.MusicKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessQueueTest {
    private fun track(id: String) = Music(id, id, "a", MusicKind.Track)
    private fun spotify(n: Int) = track("spotify:track:$n")

    @Test fun extendsOnlyNearTheEndOfAFullyLoadedContext() {
        assertTrue(EndlessQueue.shouldExtend(remaining = 2, contextLeft = 0))
        assertFalse(EndlessQueue.shouldExtend(remaining = 3, contextLeft = 0))
        assertFalse(EndlessQueue.shouldExtend(remaining = 0, contextLeft = 40))
    }

    @Test fun seedsStartWithTheLastTrackThenSpreadOverTheContext() {
        val context = (0 until 20).map(::spotify) + track("local:x")
        assertEquals(listOf(19, 0, 4, 9, 14).map { "spotify:track:$it" }, EndlessQueue.seeds(context).map(Music::id))
    }

    @Test fun seedsAreStreamedTracksOnly() {
        val queue = listOf(track("ytm:track:x"), spotify(1), track("spotify:episode:e"), track("local:3"))
        assertEquals(listOf("spotify:track:1", "ytm:track:x"), EndlessQueue.seeds(queue).map(Music::id))
        assertTrue(EndlessQueue.seeds(listOf(track("local:3"))).isEmpty())
        assertTrue(EndlessQueue.seeds(emptyList()).isEmpty())
    }

    @Test fun aRepeatedLastTrackIsOnlyOneSeed() {
        assertEquals(listOf("spotify:track:1", "spotify:track:2"), EndlessQueue.seeds(listOf(spotify(1), spotify(2), spotify(1))).map(Music::id))
    }

    @Test fun pickSkipsQueuedAndRecentTracks() {
        val candidates = (1..6).map(::spotify)
        assertEquals(listOf(2, 3, 5, 6).map { "spotify:track:$it" }, EndlessQueue.pick(candidates, listOf(spotify(1), spotify(4))).map(Music::id))
    }

    @Test fun pickKeepsEachTrackOnceAndOnlyStreamedTracks() {
        val candidates = listOf(spotify(1), spotify(1), track("ytm:track:x"), spotify(2), track("spotify:episode:e"), track("ytm:track:x"))
        assertEquals(listOf("spotify:track:1", "ytm:track:x", "spotify:track:2"), EndlessQueue.pick(candidates, emptyList()).map(Music::id))
    }

    @Test fun theSameSongFromAnotherSourceCountsOnce() {
        val playing = Music("spotify:track:1", "Feel Good Inc.", "Gorillaz, De La Soul", MusicKind.Track)
        val candidates = listOf(
            Music("ytm:track:a", "Feel Good Inc. ", "gorillaz", MusicKind.Track),
            Music("ytm:track:b", "DARE", "Gorillaz", MusicKind.Track),
            Music("ytm:track:c", "Dare", "Gorillaz", MusicKind.Track),
        )
        assertEquals(listOf("ytm:track:b"), EndlessQueue.pick(candidates, listOf(playing)).map(Music::id))
    }

    @Test fun pickStopsAtTheLimit() {
        assertEquals(EndlessQueue.BATCH, EndlessQueue.pick((1..50).map(::spotify), emptyList()).size)
        assertEquals(2, EndlessQueue.pick((1..50).map(::spotify), emptyList(), limit = 2).size)
    }

    @Test fun nothingNewMeansNothingToAdd() {
        val candidates = listOf(spotify(1), spotify(2))
        assertTrue(EndlessQueue.pick(candidates, candidates).isEmpty())
    }
}
