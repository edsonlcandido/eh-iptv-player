package app.ehtudo.domain.usecase

import app.ehtudo.domain.model.ContentType
import app.ehtudo.domain.model.PlaybackHistory
import app.ehtudo.domain.model.Result
import app.ehtudo.domain.repository.PlaybackHistoryRepository
import app.ehtudo.domain.util.isPlaybackComplete
import javax.inject.Inject

class MarkAsWatched @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) {
    suspend operator fun invoke(history: PlaybackHistory): Result<Unit> {
        val normalizedHistory = if (
            history.contentType != ContentType.LIVE &&
            isPlaybackComplete(history.resumePositionMs, history.totalDurationMs)
        ) {
            history.copy(
                resumePositionMs = history.totalDurationMs.coerceAtLeast(history.resumePositionMs),
                lastWatchedAt = System.currentTimeMillis()
            )
        } else {
            history
        }
        return playbackHistoryRepository.markAsWatched(normalizedHistory)
    }
}