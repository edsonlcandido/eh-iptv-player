package app.ehtudo.data.repository

import app.ehtudo.domain.model.ExternalRatings
import app.ehtudo.domain.model.ExternalRatingsLookup
import app.ehtudo.domain.model.Result
import app.ehtudo.domain.repository.ExternalRatingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalRatingsRepositoryImpl @Inject constructor() : ExternalRatingsRepository {

    override suspend fun getRatings(lookup: ExternalRatingsLookup): Result<ExternalRatings> {
        return Result.success(ExternalRatings.unavailable())
    }
}