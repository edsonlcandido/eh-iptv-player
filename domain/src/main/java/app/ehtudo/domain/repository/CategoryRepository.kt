package app.ehtudo.domain.repository

import app.ehtudo.domain.model.Category
import app.ehtudo.domain.model.ContentType
import app.ehtudo.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(providerId: Long): Flow<List<Category>>
    suspend fun setCategoryProtection(
        providerId: Long,
        categoryId: Long,
        type: ContentType,
        isProtected: Boolean
    ): Result<Unit>
}
