package fr.spotifeur.playback

import androidx.media3.common.Player
import fr.spotifeur.data.Music

/** The endless queue: similar Spotify tracks added when the queue is about to run out. */
internal object EndlessQueue {
    /** Fewer tracks than this after the current one starts a refill. */
    const val THRESHOLD = 3
    /** Tracks added per refill. */
    const val BATCH = 10
    /** Recent plays that are not added again. */
    const val HISTORY = 50

    /** Only once the whole context has been loaded and nothing would repeat it. */
    fun shouldExtend(remaining: Int, contextLeft: Int, repeatMode: Int): Boolean =
        contextLeft == 0 && repeatMode == Player.REPEAT_MODE_OFF && remaining < THRESHOLD

    /** The most recent Spotify track of the queue seeds the radio. */
    fun seed(queue: List<Music>): Music? = queue.lastOrNull { it.id.startsWith(SPOTIFY_TRACK) }

    /** Spotify tracks not already queued or recently played, each once, in the radio's order. */
    fun pick(candidates: List<Music>, excluded: Set<String>, limit: Int = BATCH): List<Music> =
        candidates.asSequence().filter { it.id.startsWith(SPOTIFY_TRACK) && it.id !in excluded }
            .distinctBy(Music::id).take(limit).toList()

    private const val SPOTIFY_TRACK = "spotify:track:"
}
