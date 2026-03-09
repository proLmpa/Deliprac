package com.example.baemin.service.store

import com.example.baemin.common.security.UserPrincipal
import com.example.baemin.common.security.UserRole
import com.example.baemin.dto.store.CreateStoreCommand
import com.example.baemin.dto.store.UpdateStoreCommand
import com.example.baemin.entity.store.Store
import com.example.baemin.entity.store.StoreStatus
import com.example.baemin.repository.store.StoreRepository
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
        val command = makeCreateCommand()
        val saved = makeStore()
        given(storeRepository.existsByUserIdAndName(ownerId, command.name)).willReturn(false)
        given(storeRepository.save(any(Store::class.java))).willReturn(saved)

        val result = storeService.create(command, ownerPrincipal)

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
    fun `create - duplicate name throws IllegalStateException`() {
        val command = makeCreateCommand()
        given(storeRepository.existsByUserIdAndName(ownerId, command.name)).willReturn(true)

        assertThatThrownBy { storeService.create(command, ownerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Store with that name already exists")
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
    fun `findMine - returns all stores for owner`() {
        val stores = listOf(makeStore(id = 10L), makeStore(id = 11L))
        given(storeRepository.findByUserId(ownerId)).willReturn(stores)

        val result = storeService.findMine(ownerPrincipal)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(10L, 11L)
    }

    @Test
    fun `findMine - returns empty list when owner has no stores`() {
        given(storeRepository.findByUserId(ownerId)).willReturn(emptyList())

        val result = storeService.findMine(ownerPrincipal)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findMine - non-OWNER role throws IllegalStateException`() {
        assertThatThrownBy { storeService.findMine(customerPrincipal) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only OWNER can access this")
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
