package fr.spotifeur.playback

import androidx.media3.common.Player
import fr.spotifeur.data.Music

/** The endless queue: similar tracks added when the queue is about to run out. */
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

    /** Seeds per refill: the last track, then tracks spread over the rest of the context. */
    const val SEEDS = 5

    /**
     * Tracks that seed similar tracks, so the whole playlist or album counts and not only its end:
     * the most recent one first, then others spread across the context. A seed is a YouTube Music
     * track, or a Spotify track when a Spotify account is signed in (its radio needs the session).
     */
    fun seeds(context: List<Music>, spotify: Boolean, count: Int = SEEDS): List<Music> {
        val eligible = context.filter { it.id.startsWith(YOUTUBE_TRACK) || (spotify && it.id.startsWith(SPOTIFY_TRACK)) }
        val last = eligible.lastOrNull() ?: return emptyList()
        val rest = eligible.filter { it.id != last.id }.distinctBy(Music::id)
        return listOf(last) + fr.spotifeur.data.spreadAlbumSeeds(rest, count - 1)
    }

    /** Tracks not already queued or recently played, each once, in the source's order. */
    fun pick(candidates: List<Music>, excluded: Set<String>, limit: Int = BATCH): List<Music> =
        candidates.asSequence()
            .filter { (it.id.startsWith(SPOTIFY_TRACK) || it.id.startsWith(YOUTUBE_TRACK)) && it.id !in excluded }
            .distinctBy(Music::id).take(limit).toList()

    private const val SPOTIFY_TRACK = "spotify:track:"
    private const val YOUTUBE_TRACK = fr.spotifeur.data.youtube.YTM_TRACK_PREFIX
}
