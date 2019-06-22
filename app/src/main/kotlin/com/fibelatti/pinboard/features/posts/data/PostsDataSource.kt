package com.fibelatti.pinboard.features.posts.data

import com.fibelatti.core.functional.Result
import com.fibelatti.core.functional.catching
import com.fibelatti.core.functional.getOrDefault
import com.fibelatti.core.functional.getOrNull
import com.fibelatti.core.functional.mapCatching
import com.fibelatti.core.functional.onSuccess
import com.fibelatti.pinboard.core.AppConfig.PinboardApiLiterals
import com.fibelatti.pinboard.core.functional.resultFrom
import com.fibelatti.pinboard.core.network.ApiException
import com.fibelatti.pinboard.core.util.DateFormatter
import com.fibelatti.pinboard.features.posts.data.model.ApiResultCodes
import com.fibelatti.pinboard.features.posts.data.model.GenericResponseDto
import com.fibelatti.pinboard.features.posts.data.model.PostDto
import com.fibelatti.pinboard.features.posts.data.model.PostDtoMapper
import com.fibelatti.pinboard.features.posts.data.model.SuggestedTagDtoMapper
import com.fibelatti.pinboard.features.posts.data.model.UpdateDto
import com.fibelatti.pinboard.features.posts.domain.PostsRepository
import com.fibelatti.pinboard.features.posts.domain.model.Post
import com.fibelatti.pinboard.features.posts.domain.model.SuggestedTags
import com.fibelatti.pinboard.features.tags.domain.model.Tag
import com.fibelatti.pinboard.features.user.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostsDataSource @Inject constructor(
    private val userRepository: UserRepository,
    private val postsApi: PostsApi,
    private val postsDao: PostsDao,
    private val postDtoMapper: PostDtoMapper,
    private val suggestedTagDtoMapper: SuggestedTagDtoMapper,
    private val dateFormatter: DateFormatter
) : PostsRepository {

    override suspend fun update(): Result<String> = withContext(Dispatchers.IO) {
        resultFrom(postsApi::update)
            .mapCatching(UpdateDto::updateTime)
    }

    override suspend fun add(
        url: String,
        description: String,
        extended: String?,
        private: Boolean?,
        readLater: Boolean?,
        tags: List<Tag>?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultFrom {
            postsApi.add(
                url = url,
                description = description,
                extended = extended,
                public = private?.let { if (private) PinboardApiLiterals.NO else PinboardApiLiterals.YES },
                readLater = readLater?.let { if (readLater) PinboardApiLiterals.YES else PinboardApiLiterals.NO },
                tags = tags?.forRequest()
            )
        }.orThrow()
    }

    override suspend fun delete(
        url: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultFrom { postsApi.delete(url) }
            .orThrow()
    }

    override suspend fun getRecentPosts(
        tags: List<Tag>?
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        resultFrom { postsApi.getRecentPosts(tags?.forRequest()) }
            .mapCatching { postDtoMapper.mapList(it.posts) }
    }

    override suspend fun getAllPosts(
        tags: List<Tag>?
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        withLocalDataSourceCheck { postsApi.getAllPosts(tags?.forRequest()) }
            .mapCatching(postDtoMapper::mapList)
    }

    override suspend fun getSuggestedTagsForUrl(
        url: String
    ): Result<SuggestedTags> = withContext(Dispatchers.IO) {
        resultFrom { postsApi.getSuggestedTagsForUrl(url) }
            .mapCatching(suggestedTagDtoMapper::map)
    }

    private fun Result<GenericResponseDto>.orThrow() = mapCatching {
        if (it.resultCode != ApiResultCodes.DONE.code) throw ApiException()
    }

    private fun List<Tag>.forRequest() = joinToString(PinboardApiLiterals.TAG_SEPARATOR_REQUEST) { it.name }

    private suspend inline fun withLocalDataSourceCheck(
        crossinline onInvalidLocalData: suspend () -> List<PostDto>
    ): Result<List<PostDto>> {
        val userLastUpdate = userRepository.getLastUpdate()
        val apiLastUpdate = update().getOrNull() ?: dateFormatter.nowAsTzFormat()
        val localPosts = catching { postsDao.getAllPosts() }

        return if (userLastUpdate == apiLastUpdate && localPosts.getOrDefault(emptyList()).isNotEmpty()) {
            localPosts
        } else {
            resultFrom { onInvalidLocalData() }
                .onSuccess {
                    catching {
                        userRepository.setLastUpdate(apiLastUpdate)
                        postsDao.deleteAllPosts()
                        postsDao.savePosts(it)
                    }
                }
        }
    }
}
