package app.ehtudo.domain.usecase

import app.ehtudo.domain.manager.ProviderSyncStateReader
import app.ehtudo.domain.model.Channel
import app.ehtudo.domain.model.Movie
import app.ehtudo.domain.model.Series
import app.ehtudo.domain.model.SyncState
import app.ehtudo.domain.repository.SearchRepository
import app.ehtudo.domain.repository.SearchRepositoryResult
import app.ehtudo.domain.util.shouldRethrowDomainFlowFailure
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull

enum class SearchContentScope {
    ALL,
    LIVE,
    MOVIES,
    SERIES
}

data class SearchContentResult(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val isPartialResult: Boolean = false
)

class SearchContent @Inject constructor(
    private val searchRepository: SearchRepository,
    private val providerSyncStateReader: ProviderSyncStateReader
) {
    private companion object {
        const val SEARCH_RESPONSE_TIMEOUT_MS = 2_500L
    }

    private val logger = Logger.getLogger("SearchContent")

    operator fun invoke(
        providerId: Long,
        query: String,
        scope: SearchContentScope = SearchContentScope.ALL,
        maxResultsPerSection: Int = 120
    ): Flow<SearchContentResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) {
            return flowOf(SearchContentResult())
        }

        return combine(
            contentSearchFlow(providerId, normalizedQuery, scope, maxResultsPerSection),
            providerSyncStateReader.observeBackgroundIndexingActive(providerId)
        ) { searchResult, indexWorkActive ->
            val (result, searchDegraded) = searchResult
            val indexingActive = when (providerSyncStateReader.currentSyncState(providerId)) {
                is SyncState.Syncing,
                is SyncState.Partial -> true
                else -> false
            }
            SearchContentResult(
                channels = result.channels,
                movies = result.movies,
                series = result.series,
                isPartialResult = searchDegraded || indexingActive || indexWorkActive
            )
        }
    }

    private fun contentSearchFlow(
        providerId: Long,
        query: String,
        scope: SearchContentScope,
        maxResultsPerSection: Int
    ): Flow<Pair<SearchRepositoryResult, Boolean>> =
        flow {
            val result = withTimeoutOrNull(SEARCH_RESPONSE_TIMEOUT_MS) {
                searchRepository.searchContent(
                    providerId = providerId,
                    query = query,
                    includeLive = scope == SearchContentScope.ALL || scope == SearchContentScope.LIVE,
                    includeMovies = scope == SearchContentScope.ALL || scope == SearchContentScope.MOVIES,
                    includeSeries = scope == SearchContentScope.ALL || scope == SearchContentScope.SERIES,
                    maxResultsPerSection = maxResultsPerSection
                ).first()
            }

            if (result == null) {
                logger.warning("Search timed out for provider $providerId and query '$query'")
                emit(SearchRepositoryResult() to true)
            } else {
                emit(result to false)
            }
        }
            .catch { error ->
                if (error.shouldRethrowDomainFlowFailure()) throw error
                logger.log(Level.WARNING, "Unified content search failed", error)
                emit(SearchRepositoryResult() to true)
            }
}
