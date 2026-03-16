package store.service.product

import common.security.UserPrincipal
import common.security.UserRole
import store.dto.product.CreateProductRequest
import store.dto.product.UpdateProductRequest
import store.entity.product.Product
import store.entity.store.Store
import store.entity.store.StoreStatus
import store.repository.product.ProductRepository
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
class ProductServiceTest {

    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var storeRepository: StoreRepository
    @InjectMocks private lateinit var productService: ProductService

    private val ownerId    = 1L
    private val storeId    = 10L
    private val productId  = 100L
    private val ownerPrincipal    = UserPrincipal(ownerId, UserRole.OWNER)
    private val customerPrincipal = UserPrincipal(2L,      UserRole.CUSTOMER)

    private fun makeStore(userId: Long = ownerId) = Store(
        id = storeId, userId = userId, name = "Test Store", address = "Seoul",
        phone = "02-1234-5678", content = "Good food", status = StoreStatus.ACTIVE,
        productCreatedTime = 32400000L, openedTime = 36000000L, closedTime = 79200000L,
        closedDays = "MONDAY", createdAt = 0L, updatedAt = 0L
    )

    private fun makeProduct(storeId: Long = this.storeId, popularity: Long = 0L) = Product(
        id = productId, storeId = storeId, name = "Burger", description = "Tasty burger",
        price = 8000L, productPictureUrl = null, popularity = popularity, status = true,
        createdAt = 0L, updatedAt = 0L
    )

    private fun makeCreateRequest() = CreateProductRequest(
        storeId = storeId, name = "Burger", description = "Tasty burger", price = 8000L, productPictureUrl = null
    )

    private fun makeUpdateRequest() = UpdateProductRequest(
        storeId = storeId, productId = productId,
        name = "Updated Burger", description = "Even tastier", price = 9000L, productPictureUrl = null
    )

    // --- create ---

    @Test
    fun `create - happy path returns ProductInfo`() {
        val saved = makeProduct()
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.save(any(Product::class.java))).willReturn(saved)

        val result = productService.create(storeId, makeCreateRequest(), ownerPrincipal)

        assertThat(result.id).isEqualTo(productId)
        assertThat(result.name).isEqualTo("Burger")
        assertThat(result.status).isTrue()
        assertThat(result.popularity).isEqualTo(0L)
    }

    @Test
    fun `create - non-OWNER role throws IllegalStateException`() {
        assertThatThrownBy { productService.create(storeId, makeCreateRequest(), customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can create products")
    }

    @Test
    fun `create - store not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.empty())

        assertThatThrownBy { productService.create(storeId, makeCreateRequest(), ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    @Test
    fun `create - wrong owner throws IllegalStateException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore(userId = 99L)))

        assertThatThrownBy { productService.create(storeId, makeCreateRequest(), ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    // --- listByStore ---

    @Test
    fun `listByStore - returns list of ProductInfo`() {
        given(productRepository.findAllByStoreId(storeId)).willReturn(listOf(makeProduct(), makeProduct()))

        val result = productService.listByStore(storeId)

        assertThat(result).hasSize(2)
    }

    // --- findById ---

    @Test
    fun `findById - happy path returns ProductInfo`() {
        given(productRepository.findById(productId)).willReturn(Optional.of(makeProduct()))

        val result = productService.findById(storeId, productId)

        assertThat(result.id).isEqualTo(productId)
    }

    @Test
    fun `findById - product not found throws IllegalArgumentException`() {
        given(productRepository.findById(productId)).willReturn(Optional.empty())

        assertThatThrownBy { productService.findById(storeId, productId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Product not found")
    }

    @Test
    fun `findById - product belongs to different store throws IllegalArgumentException`() {
        given(productRepository.findById(productId)).willReturn(Optional.of(makeProduct(storeId = 999L)))

        assertThatThrownBy { productService.findById(storeId, productId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Product not found in this store")
    }

    // --- update ---

    @Test
    fun `update - happy path returns updated ProductInfo`() {
        val product = makeProduct()
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findById(productId)).willReturn(Optional.of(product))
        given(productRepository.save(any(Product::class.java))).willReturn(product)

        val result = productService.update(storeId, productId, makeUpdateRequest(), ownerId)

        assertThat(result.name).isEqualTo("Updated Burger")
        assertThat(result.price).isEqualTo(9000L)
        then(productRepository).should().save(product)
    }

    @Test
    fun `update - store not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.empty())

        assertThatThrownBy { productService.update(storeId, productId, makeUpdateRequest(), ownerId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    @Test
    fun `update - wrong owner throws IllegalStateException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore(userId = 99L)))

        assertThatThrownBy { productService.update(storeId, productId, makeUpdateRequest(), ownerId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    @Test
    fun `update - product not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findById(productId)).willReturn(Optional.empty())

        assertThatThrownBy { productService.update(storeId, productId, makeUpdateRequest(), ownerId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Product not found")
    }

    // --- deactivate ---

    @Test
    fun `deactivate - happy path sets status to false`() {
        val product = makeProduct()
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findById(productId)).willReturn(Optional.of(product))
        given(productRepository.save(any(Product::class.java))).willReturn(product)

        productService.deactivate(storeId, productId, ownerId)

        assertThat(product.status).isFalse()
        then(productRepository).should().save(product)
    }

    @Test
    fun `deactivate - wrong owner throws IllegalStateException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore(userId = 99L)))

        assertThatThrownBy { productService.deactivate(storeId, productId, ownerId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    @Test
    fun `deactivate - product not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findById(productId)).willReturn(Optional.empty())

        assertThatThrownBy { productService.deactivate(storeId, productId, ownerId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Product not found")
    }

    // --- incrementPopularity ---

    @Test
    fun `incrementPopularity - increases popularity by delta`() {
        val product = makeProduct(popularity = 3L)
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findById(productId)).willReturn(Optional.of(product))
        given(productRepository.save(any(Product::class.java))).willReturn(product)

        productService.incrementPopularity(storeId, productId, 5, ownerId)

        assertThat(product.popularity).isEqualTo(8L)
    }
}

@ExtendWith(MockitoExtension::class)
class ProductStatisticsServiceTest {

    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var storeRepository: StoreRepository
    @InjectMocks private lateinit var productStatisticsService: ProductStatisticsService

    private val ownerId   = 1L
    private val storeId   = 10L
    private val productId = 100L
    private val ownerPrincipal    = UserPrincipal(ownerId, UserRole.OWNER)
    private val customerPrincipal = UserPrincipal(2L, UserRole.CUSTOMER)

    private fun makeStore(userId: Long = ownerId) = Store(
        id = storeId, userId = userId, name = "Test Store", address = "Seoul",
        phone = "02-1234-5678", content = "Good food", status = StoreStatus.ACTIVE,
        productCreatedTime = 32400000L, openedTime = 36000000L, closedTime = 79200000L,
        closedDays = "MONDAY", createdAt = 0L, updatedAt = 0L
    )

    private fun makeProduct() = Product(
        id = productId, storeId = storeId, name = "Burger", description = "Tasty burger",
        price = 8000L, productPictureUrl = null, popularity = 10L, status = true,
        createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun `getPopularProducts - happy path returns sorted list`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore()))
        given(productRepository.findTopByStoreIdOrderByPopularityDesc(storeId, 5L)).willReturn(listOf(makeProduct()))

        val result = productStatisticsService.getPopularProducts(storeId, ownerPrincipal)

        assertThat(result).hasSize(1)
        assertThat(result[0].popularity).isEqualTo(10L)
    }

    @Test
    fun `getPopularProducts - store not found throws IllegalArgumentException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.empty())

        assertThatThrownBy { productStatisticsService.getPopularProducts(storeId, ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    @Test
    fun `getPopularProducts - non-OWNER role throws IllegalStateException`() {
        assertThatThrownBy { productStatisticsService.getPopularProducts(storeId, customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can view store statistics")
    }

    @Test
    fun `getPopularProducts - wrong owner throws IllegalStateException`() {
        given(storeRepository.findById(storeId)).willReturn(Optional.of(makeStore(userId = 99L)))

        assertThatThrownBy { productStatisticsService.getPopularProducts(storeId, ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }
}
