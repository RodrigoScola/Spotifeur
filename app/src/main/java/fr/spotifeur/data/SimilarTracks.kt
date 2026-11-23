package fr.spotifeur.data

import kotlinx.coroutines.CancellationException

/**
 * A service that suggests tracks similar to a track, for the endless queue. Supporting another service
 * means implementing this and adding it to the model's list of sources; nothing else changes.
 */
interface SimilarTracksSource {
    /** Whether this source can suggest tracks from [seed]. */
    fun accepts(seed: Music): Boolean
    /** Tracks similar to [seed], empty when there are none. A failure lets the next source answer. */
    suspend fun similarTo(seed: Music): List<Music>
}

/**
 * For each seed, the tracks of the first source that accepts it and finds some: a later source is the
 * fallback of an earlier one. Seeds are read one after the other, so no service gets a burst of requests.
 */
internal suspend fun similarTracks(seeds: List<Music>, sources: List<SimilarTracksSource>): List<List<Music>> =
    seeds.map { seed ->
        sources.filter { it.accepts(seed) }.firstNotNullOfOrNull { source ->
            try { source.similarTo(seed).ifEmpty { null } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }.orEmpty()
    }
