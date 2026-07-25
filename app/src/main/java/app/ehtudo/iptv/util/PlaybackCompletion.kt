package app.ehtudo.iptv.util

import app.ehtudo.domain.util.isPlaybackComplete as domainIsPlaybackComplete

fun isPlaybackComplete(
    progressMs: Long,
    totalDurationMs: Long,
    threshold: Float = app.ehtudo.domain.util.DEFAULT_PLAYBACK_COMPLETION_THRESHOLD
): Boolean = domainIsPlaybackComplete(progressMs, totalDurationMs, threshold)
