package store.api.store

import common.security.currentUser
import store.dto.store.CreateStoreCommand
import store.dto.store.CreateStoreRequest
import store.dto.store.DeactivateStoreRequest
import store.dto.store.FindStoreRequest
import store.dto.store.ListStoreRequest
import store.dto.store.StoreSortBy
import store.dto.store.StoreResponse
import store.dto.store.UpdateStoreCommand
import store.dto.store.UpdateStoreRequest
import store.service.store.StoreService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreController(private val storeService: StoreService) {

    @PostMapping("/api/stores")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateStoreRequest): StoreResponse {
        val command = CreateStoreCommand(
            request.name,
            request.address,
            request.phone,
            request.content,
            request.storePictureUrl,
            request.productCreatedTime,
            request.openedTime,
            request.closedTime,
            request.closedDays
        )
        val store = storeService.create(command, currentUser())
        return StoreResponse.of(store)
    }

    @PostMapping("/api/stores/list")
    fun listAll(@RequestBody request: ListStoreRequest): List<StoreResponse> {
        val sort = try { StoreSortBy.valueOf(request.sortBy) }
                   catch (e: IllegalArgumentException) { throw IllegalArgumentException("Invalid sortBy: must be CREATED_AT or RATING") }
        return storeService.listAll(sort).map { StoreResponse.of(it) }
    }

    @PostMapping("/api/stores/mine")
    fun mine(): List<StoreResponse> {
        return storeService.findMine(currentUser()).map { StoreResponse.of(it) }
    }

    @PostMapping("/api/stores/find")
    fun findById(@RequestBody request: FindStoreRequest): StoreResponse {
        return StoreResponse.of(storeService.findById(request.id))
    }

    @PutMapping("/api/stores")
    fun update(@RequestBody request: UpdateStoreRequest): StoreResponse {
        val command = UpdateStoreCommand(
            request.name,
            request.address,
            request.phone,
            request.content,
            request.storePictureUrl,
            request.productCreatedTime,
            request.openedTime,
            request.closedTime,
            request.closedDays
        )
        val store = storeService.update(request.id, command, currentUser().id)
        return StoreResponse.of(store)
    }

    @PutMapping("/api/stores/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@RequestBody request: DeactivateStoreRequest) {
        storeService.deactivate(request.id, currentUser().id)
    }
}
