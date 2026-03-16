package store.service.review

import common.security.UserPrincipal
import common.security.UserRole
import common.exception.ForbiddenException
import common.exception.NotFoundException
import store.dto.review.CreateReviewRequest
import store.entity.review.Review
import store.entity.store.Store
import store.entity.store.StoreStatus
import store.repository.review.ReviewRepository
import store.repository.store.StoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ReviewServiceTest {

    @Mock private lateinit var reviewRepository: ReviewRepository
    @Mock private lateinit var storeRepository: StoreRepository
    @InjectMocks private lateinit var reviewService: ReviewService

    private val customerId        = 1L
    private val storeId           = 10L
    private val reviewId          = 100L
    private val customerPrincipal = UserPrincipal(customerId, UserRole.CUSTOMER)
    private val ownerPrincipal    = UserPrincipal(2L, UserRole.OWNER)

    private fun makeStore() = Store(
        id = storeId, userId = 99L, name = "Test Store", address = "Seoul",
        phone = "02-1234-5678", content = "Good food", status = StoreStatus.ACTIVE,
        productCreatedTime = 32400000L, openedTime = 36000000L, closedTime = 79200000L,
        closedDays = "MONDAY",
    )

    private fun makeReview(userId: Long = customerId, storeId: Long = this.storeId) = Review(
        id = reviewId, storeId = storeId, userId = userId,
        rating = 5, content = "Great food!",
    )

    private fun makeCreateRequest() = CreateReviewRequest(storeId = storeId, rating = 5, content = "Great food!")

    // --- create ---

    @Test
    fun `create - happy path returns ReviewInfo`() {
        val saved = makeReview()
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(reviewRepository.save(any(Review::class.java))).willReturn(saved)

        val result = reviewService.create(storeId, makeCreateRequest(), customerPrincipal)

        assertThat(result.id).isEqualTo(reviewId)
        assertThat(result.rating).isEqualTo(5)
        assertThat(result.content).isEqualTo("Great food!")
    }

    @Test
    fun `create - non-CUSTOMER role throws IllegalStateException`() {
        assertThatThrownBy { reviewService.create(storeId, makeCreateRequest(), ownerPrincipal) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Only CUSTOMER can create reviews")
    }

    @Test
    fun `create - invalid rating throws IllegalArgumentException`() {
        val badRequest = CreateReviewRequest(storeId = storeId, rating = 6, content = "Too good")

        assertThatThrownBy { reviewService.create(storeId, badRequest, customerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Rating must be between 1 and 5")
    }

    @Test
    fun `create - store not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.empty())

        assertThatThrownBy { reviewService.create(storeId, makeCreateRequest(), customerPrincipal) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Store not found")
    }

    // --- listByStore ---

    @Test
    fun `listByStore - returns list of ReviewInfo`() {
        given(reviewRepository.findAllByStoreId(storeId)).willReturn(listOf(makeReview(), makeReview()))

        val result = reviewService.listByStore(storeId)

        assertThat(result).hasSize(2)
    }

    // --- delete ---

    @Test
    fun `delete - happy path deletes review`() {
        val review = makeReview()
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))

        reviewService.delete(storeId, reviewId, customerPrincipal)

        then(reviewRepository).should().delete(review)
    }

    @Test
    fun `delete - review not found throws IllegalArgumentException`() {
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty())

        assertThatThrownBy { reviewService.delete(storeId, reviewId, customerPrincipal) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Review not found")
    }

    @Test
    fun `delete - review belongs to different store throws IllegalArgumentException`() {
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(makeReview(storeId = 999L)))

        assertThatThrownBy { reviewService.delete(storeId, reviewId, customerPrincipal) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Review not found in this store")
    }

    @Test
    fun `delete - wrong user throws IllegalStateException`() {
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(makeReview(userId = 999L)))

        assertThatThrownBy { reviewService.delete(storeId, reviewId, customerPrincipal) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Forbidden")
    }

    @Test
    fun `delete - ADMIN can delete another user's review`() {
        val adminPrincipal = UserPrincipal(99L, UserRole.ADMIN)
        val review = makeReview(userId = 999L)
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))

        reviewService.delete(storeId, reviewId, adminPrincipal)

        then(reviewRepository).should().delete(review)
    }
}
