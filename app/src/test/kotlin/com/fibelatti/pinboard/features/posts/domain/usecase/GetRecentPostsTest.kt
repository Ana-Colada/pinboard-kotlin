package com.fibelatti.pinboard.features.posts.domain.usecase

import com.fibelatti.core.functional.Failure
import com.fibelatti.core.functional.Success
import com.fibelatti.core.functional.exceptionOrNull
import com.fibelatti.core.functional.getOrNull
import com.fibelatti.core.test.extension.callSuspend
import com.fibelatti.core.test.extension.givenSuspend
import com.fibelatti.core.test.extension.mock
import com.fibelatti.core.test.extension.safeAny
import com.fibelatti.core.test.extension.shouldBe
import com.fibelatti.core.test.extension.shouldBeAnInstanceOf
import com.fibelatti.core.test.extension.verifySuspend
import com.fibelatti.pinboard.MockDataProvider.createPost
import com.fibelatti.pinboard.MockDataProvider.mockTag1
import com.fibelatti.pinboard.MockDataProvider.mockTag2
import com.fibelatti.pinboard.MockDataProvider.mockTag3
import com.fibelatti.pinboard.MockDataProvider.mockTag4
import com.fibelatti.pinboard.MockDataProvider.mockTags
import com.fibelatti.pinboard.MockDataProvider.mockTime1
import com.fibelatti.pinboard.MockDataProvider.mockTime2
import com.fibelatti.pinboard.MockDataProvider.mockTime3
import com.fibelatti.pinboard.MockDataProvider.mockTime4
import com.fibelatti.pinboard.features.appstate.NewestFirst
import com.fibelatti.pinboard.features.posts.domain.PostsRepository
import com.fibelatti.pinboard.features.posts.domain.model.Post
import com.fibelatti.pinboard.features.tags.domain.model.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never

class GetRecentPostsTest {

    // region Mock data
    private val mockResponseFull = listOf(
        createPost(time = mockTime1, tags = listOf(mockTag1)),
        createPost(time = mockTime2, tags = listOf(mockTag2)),
        createPost(time = mockTime3, tags = listOf(mockTag3)),
        createPost(time = mockTime4, tags = listOf(mockTag4))
    )

    private val mockSortType = NewestFirst
    // endregion

    private val mockPostsRepository = mock<PostsRepository>()
    private val mockFilterPosts = mock<FilterPosts>()
    private val mockSort = mock<Sort>()

    private val getRecentPosts = GetRecentPosts(
        mockPostsRepository,
        mockFilterPosts,
        mockSort
    )

    @Test
    fun `GIVEN no tags are sent as a parameter WHEN GetRecentPosts is called THEN all posts are returned`() {
        // GIVEN
        val params = GetParams(sorting = mockSortType)
        givenSuspend { mockPostsRepository.getRecentPosts() }
            .willReturn(Success(mockResponseFull))
        arrangeFilterAndSort(tags = emptyList())

        // WHEN
        val result = callSuspend { getRecentPosts(params) }

        // THEN
        result.shouldBeAnInstanceOf<Success<List<Post>>>()
        result.getOrNull() shouldBe mockResponseFull
    }

    @Test
    fun `GIVEN repository fails WHEN GetRecentPosts is called THEN Failure is returned`() {
        // GIVEN
        val params = GetParams()
        givenSuspend { mockPostsRepository.getRecentPosts() }
            .willReturn(Failure(Exception()))

        // WHEN
        val result = callSuspend { getRecentPosts(params) }

        // THEN
        result.shouldBeAnInstanceOf<Failure>()
        result.exceptionOrNull()?.shouldBeAnInstanceOf<Exception>()

        verifySuspend(mockFilterPosts, never()) { invoke(safeAny()) }
        verifySuspend(mockSort, never()) { invoke(safeAny()) }
    }

    private fun arrangeFilterAndSort(
        tags: List<Tag> = mockTags
    ) {
        givenSuspend { mockFilterPosts(FilterPosts.Params(mockResponseFull, term = "", tags = tags)) }
            .willReturn(Success(mockResponseFull))
        givenSuspend { mockSort(Sort.Params(mockResponseFull, mockSortType)) }
            .willReturn(Success(mockResponseFull))
    }
}
