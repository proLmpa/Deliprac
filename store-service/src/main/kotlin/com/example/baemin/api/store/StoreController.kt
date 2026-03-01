package com.example.baemin.api.store

import com.example.baemin.common.security.currentUser
import com.example.baemin.dto.store.CreateStoreCommand
import com.example.baemin.dto.store.CreateStoreRequest
import com.example.baemin.dto.store.StoreResponse
import com.example.baemin.dto.store.UpdateStoreCommand
import com.example.baemin.dto.store.UpdateStoreRequest
import com.example.baemin.service.store.StoreService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreController(private val storeService: StoreService) {

    @PostMapping("/api/stores")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateStoreRequest): StoreResponse {
        return StoreResponse.of(storeService.create(CreateStoreCommand(request.name, request.address, request.phone, request.content, request.storePictureUrl, request.productCreatedTime, request.openedTime, request.closedTime, request.closedDays), currentUser()))
    }

    @GetMapping("/api/stores")
    fun listAll(): List<StoreResponse> {
        return storeService.listAll().map { StoreResponse.of(it) }
    }

    @GetMapping("/api/stores/mine")
    fun mine(): StoreResponse {
        return StoreResponse.of(storeService.findMine(currentUser()))
    }
    @GetMapping("/api/stores/{id}")
    fun findById(@PathVariable id: Long): StoreResponse {
        return StoreResponse.of(storeService.findById(id))
    }

    @PutMapping("/api/stores/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: UpdateStoreRequest): StoreResponse {

        return StoreResponse.of(storeService.update(id, UpdateStoreCommand(request.name, request.address, request.phone, request.content, request.storePictureUrl, request.productCreatedTime, request.openedTime, request.closedTime, request.closedDays), currentUser()))
    }
    @PutMapping("/api/stores/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@PathVariable id: Long) = storeService.deactivate(id, currentUser())
}
