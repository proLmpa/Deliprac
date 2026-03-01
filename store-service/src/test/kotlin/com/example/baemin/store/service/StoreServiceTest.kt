package com.example.baemin.store.service

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.store.dto.CreateStoreCommand
import com.example.baemin.store.dto.UpdateStoreCommand
import com.example.baemin.store.entity.Store
import com.example.baemin.store.entity.StoreStatus
import com.example.baemin.store.repository.StoreRepository
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
class StoreServiceTest {

    @Mock private lateinit var storeRepository: StoreRepository
    @InjectMocks private lateinit var storeService: StoreService

    private val ownerId = 1L
    private val ownerPrincipal = UserPrincipal(ownerId, "owner@example.com", UserRole.OWNER)
    private val customerPrincipal = UserPrincipal(2L, "customer@example.com", UserRole.CUSTOMER)

    private fun makeStore(userId: Long = ownerId, id: Long = 10L, status: StoreStatus = StoreStatus.ACTIVE) = Store(
        id                 = id,
        userId             = userId,
        name               = "Test Store",
        address            = "Seoul Gangnam-gu",
        phone              = "02-1234-5678",
        content            = "Good food",
        status             = status,
        productCreatedTime = 32400000L,
        openedTime         = 36000000L,
        closedTime         = 79200000L,
        closedDays         = "MONDAY",
        createdAt          = 0L,
        updatedAt          = 0L
    )

    private fun makeCreateCommand() = CreateStoreCommand(
        name               = "Test Store",
        address            = "Seoul Gangnam-gu",
        phone              = "02-1234-5678",
        content            = "Good food",
        storePictureUrl    = null,
        productCreatedTime = 32400000L,
        openedTime         = 36000000L,
        closedTime         = 79200000L,
        closedDays         = "MONDAY"
    )

    private fun makeUpdateCommand() = UpdateStoreCommand(
        name               = "Updated Store",
        address            = "Seoul Mapo-gu",
        phone              = "02-9876-5432",
        content            = "Even better food",
        storePictureUrl    = "https://example.com/pic.jpg",
        productCreatedTime = 28800000L,
        openedTime         = 32400000L,
        closedTime         = 75600000L,
        closedDays         = "SUNDAY"
    )

    // --- create ---

    @Test
    fun `create - happy path returns StoreInfo`() {
        val saved = makeStore()
        given(storeRepository.existsByUserId(ownerId)).willReturn(false)
        given(storeRepository.save(any(Store::class.java))).willReturn(saved)

        val result = storeService.create(makeCreateCommand(), ownerPrincipal)

        assertThat(result.id).isEqualTo(saved.id)
        assertThat(result.name).isEqualTo("Test Store")
        assertThat(result.status).isEqualTo("ACTIVE")
    }

    @Test
    fun `create - non-OWNER role throws IllegalStateException`() {
        assertThatThrownBy { storeService.create(makeCreateCommand(), customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can create a store")
    }

    @Test
    fun `create - store already exists throws IllegalStateException`() {
        given(storeRepository.existsByUserId(ownerId)).willReturn(true)

        assertThatThrownBy { storeService.create(makeCreateCommand(), ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Store already exists for this owner")
    }

    // --- findById ---

    @Test
    fun `findById - found returns StoreInfo`() {
        val store = makeStore()
        given(storeRepository.findById(store.id)).willReturn(Optional.of(store))

        val result = storeService.findById(store.id)

        assertThat(result.id).isEqualTo(store.id)
    }

    @Test
    fun `findById - not found throws IllegalArgumentException`() {
        val id = 99L
        given(storeRepository.findById(id)).willReturn(Optional.empty())

        assertThatThrownBy { storeService.findById(id) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    // --- findMine ---

    @Test
    fun `findMine - owner has store returns StoreInfo`() {
        val store = makeStore()
        given(storeRepository.findByUserId(ownerId)).willReturn(store)

        val result = storeService.findMine(ownerPrincipal)

        assertThat(result.id).isEqualTo(store.id)
    }

    @Test
    fun `findMine - non-OWNER role throws IllegalStateException`() {
        assertThatThrownBy { storeService.findMine(customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can access this")
    }

    @Test
    fun `findMine - store not found throws IllegalArgumentException`() {
        given(storeRepository.findByUserId(ownerId)).willReturn(null)

        assertThatThrownBy { storeService.findMine(ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    // --- update ---

    @Test
    fun `update - happy path returns updated StoreInfo`() {
        val storeId = 10L
        val store = makeStore(id = storeId)
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store))
        given(storeRepository.save(any(Store::class.java))).willReturn(store)

        val result = storeService.update(storeId, makeUpdateCommand(), ownerPrincipal)

        assertThat(result.name).isEqualTo("Updated Store")
        then(storeRepository).should().save(store)
    }

    @Test
    fun `update - store not found throws IllegalArgumentException`() {
        val id = 99L
        given(storeRepository.findById(id)).willReturn(Optional.empty())

        assertThatThrownBy { storeService.update(id, makeUpdateCommand(), ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    @Test
    fun `update - wrong owner throws IllegalStateException`() {
        val storeId = 10L
        val store = makeStore(userId = 2L, id = storeId)
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store))

        assertThatThrownBy { storeService.update(storeId, makeUpdateCommand(), ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }

    // --- deactivate ---

    @Test
    fun `deactivate - happy path sets status to INACTIVE`() {
        val storeId = 10L
        val store = makeStore(id = storeId)
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store))
        given(storeRepository.save(any(Store::class.java))).willReturn(store)

        storeService.deactivate(storeId, ownerPrincipal)

        assertThat(store.status).isEqualTo(StoreStatus.INACTIVE)
        then(storeRepository).should().save(store)
    }

    @Test
    fun `deactivate - store not found throws IllegalArgumentException`() {
        val id = 99L
        given(storeRepository.findById(id)).willReturn(Optional.empty())

        assertThatThrownBy { storeService.deactivate(id, ownerPrincipal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Store not found")
    }

    @Test
    fun `deactivate - wrong owner throws IllegalStateException`() {
        val storeId = 10L
        val store = makeStore(userId = 2L, id = storeId)
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store))

        assertThatThrownBy { storeService.deactivate(storeId, ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Forbidden")
    }
}
