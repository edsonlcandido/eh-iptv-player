package app.ehtudo.domain.repository

import app.ehtudo.domain.model.ExternalRatings
import app.ehtudo.domain.model.ExternalRatingsLookup
import app.ehtudo.domain.model.Result

interface ExternalRatingsRepository {
    suspend fun getRatings(lookup: ExternalRatingsLookup): Result<ExternalRatings>
}