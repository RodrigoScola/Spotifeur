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

    /**
     * The most recent track that can seed similar tracks: a YouTube Music track, or a Spotify
     * track when a Spotify account is signed in (its radio needs the session).
     */
    fun seed(queue: List<Music>, spotify: Boolean): Music? =
        queue.lastOrNull { it.id.startsWith(YOUTUBE_TRACK) || (spotify && it.id.startsWith(SPOTIFY_TRACK)) }

    /** Tracks not already queued or recently played, each once, in the source's order. */
    fun pick(candidates: List<Music>, excluded: Set<String>, limit: Int = BATCH): List<Music> =
        candidates.asSequence()
            .filter { (it.id.startsWith(SPOTIFY_TRACK) || it.id.startsWith(YOUTUBE_TRACK)) && it.id !in excluded }
            .distinctBy(Music::id).take(limit).toList()

    private const val SPOTIFY_TRACK = "spotify:track:"
    private const val YOUTUBE_TRACK = fr.spotifeur.data.youtube.YTM_TRACK_PREFIX
}
