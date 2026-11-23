package fr.spotifeur.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarTracksTest {
    private fun track(id: String) = Music(id, id, "a", MusicKind.Track)

    /** A source that accepts ids with [prefix] and answers [answer], or throws when it is null. */
    private class FakeSource(val prefix: String, val answer: List<Music>?) : SimilarTracksSource {
        val asked = mutableListOf<String>()
        override fun accepts(seed: Music) = seed.id.startsWith(prefix)
        override suspend fun similarTo(seed: Music): List<Music> { asked += seed.id; return answer ?: error("offline") }
    }

    @Test fun theFirstSourceThatAnswersWins() = runBlocking {
        val first = FakeSource("a:", listOf(track("x")))
        val second = FakeSource("a:", listOf(track("y")))
        assertEquals(listOf(listOf(track("x"))), similarTracks(listOf(track("a:1")), listOf(first, second)))
        assertEquals(emptyList<String>(), second.asked)
    }

    @Test fun aFailingOrEmptySourceFallsBackToTheNext() = runBlocking {
        val failing = FakeSource("a:", null)
        val empty = FakeSource("a:", emptyList())
        val fallback = FakeSource("a:", listOf(track("y")))
        assertEquals(listOf(listOf(track("y"))), similarTracks(listOf(track("a:1")), listOf(failing, empty, fallback)))
    }

    @Test fun eachSeedGoesToTheSourcesThatAcceptIt() = runBlocking {
        val a = FakeSource("a:", listOf(track("from a")))
        val b = FakeSource("b:", listOf(track("from b")))
        val stations = similarTracks(listOf(track("a:1"), track("b:1"), track("c:1")), listOf(a, b))
        assertEquals(listOf(listOf(track("from a")), listOf(track("from b")), emptyList()), stations)
        assertEquals(listOf("a:1"), a.asked)
        assertEquals(listOf("b:1"), b.asked)
    }
}
