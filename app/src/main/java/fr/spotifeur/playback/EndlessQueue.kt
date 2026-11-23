package fr.spotifeur.playback

import fr.spotifeur.data.Music
import fr.spotifeur.data.spreadAlbumSeeds

/** The endless queue: similar tracks added when the queue is about to run out. */
internal object EndlessQueue {
    /** Fewer tracks than this after the current one starts a refill. */
    const val THRESHOLD = 3
    /** Tracks added per refill. */
    const val BATCH = 10
    /** Recent plays that are not added again. */
    const val HISTORY = 50
    /** Seeds per refill: the last track, then tracks spread over the rest of the context. */
    const val SEEDS = 5

    /** Only once the whole context has been loaded. */
    fun shouldExtend(remaining: Int, contextLeft: Int): Boolean = contextLeft == 0 && remaining < THRESHOLD

    /** The last track a source [accepts], then others spread over the context, so all of it counts. */
    fun seeds(context: List<Music>, count: Int = SEEDS, accepts: (Music) -> Boolean): List<Music> {
        val tracks = context.filter(accepts)
        val last = tracks.lastOrNull() ?: return emptyList()
        return listOf(last) + spreadAlbumSeeds(tracks.filter { it.id != last.id }.distinctBy(Music::id), count - 1)
    }

    /**
     * Tracks not already queued or recently played, each once, in the sources' order. A song is the same
     * by id or by title and main artist, so its Spotify, YouTube Music song and music video versions count once.
     */
    fun pick(candidates: List<Music>, excluded: List<Music>, limit: Int = BATCH): List<Music> {
        val seen = excluded.flatMapTo(HashSet()) { listOf(it.id, song(it)) }
        return candidates.filter { seen.add(it.id) and seen.add(song(it)) }.take(limit)
    }

    private fun song(track: Music) = if (track.title.isBlank()) track.id
        else "${track.title.trim().lowercase()}|${track.subtitle.substringBefore(',').trim().lowercase()}"
}
