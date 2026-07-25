package app.ehtudo.data.remote.stalker

import app.ehtudo.domain.model.StalkerBootstrapRecipe
import app.ehtudo.domain.model.StalkerCookieMode
import app.ehtudo.domain.model.StalkerEndpointPreference
import app.ehtudo.domain.model.StalkerMagPreset
import app.ehtudo.domain.model.StalkerPlaybackBackendHint
import app.ehtudo.domain.model.StalkerPortalFingerprint
import java.io.IOException

class StalkerPlaybackResolutionException(
    message: String,
    cause: Throwable? = null,
    val streamKind: StalkerStreamKind = StalkerStreamKind.LIVE,
    val portalFingerprint: StalkerPortalFingerprint? = null,
    val magPreset: StalkerMagPreset? = null,
    val bootstrapRecipe: StalkerBootstrapRecipe? = null,
    val endpointPreference: StalkerEndpointPreference = StalkerEndpointPreference.AUTO,
    val cookieMode: StalkerCookieMode = StalkerCookieMode.NONE,
    val playbackBackendHint: StalkerPlaybackBackendHint = StalkerPlaybackBackendHint.AUTO,
    val fallbackRecipeUsed: Boolean = false,
    val rediscoveryAttempted: Boolean = false
) : IOException(message, cause)
