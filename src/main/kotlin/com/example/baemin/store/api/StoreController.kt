package com.example.baemin.store.api

import com.example.baemin.store.dto.CreateStoreRequest
import com.example.baemin.store.dto.StoreResponse
import com.example.baemin.store.dto.UpdateStoreRequest
import com.example.baemin.store.service.StoreService
import com.example.baemin.user.security.currentUser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/stores")
class StoreController(private val storeService: StoreService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateStoreRequest): StoreResponse =
        storeService.create(request, currentUser())

    @GetMapping
    fun listAll(): List<StoreResponse> = storeService.listAll()

    @GetMapping("/mine")
    fun mine(): StoreResponse = storeService.findMine(currentUser())

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): StoreResponse = storeService.findById(id)

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody request: UpdateStoreRequest): StoreResponse =
        storeService.update(id, request, currentUser())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = storeService.delete(id, currentUser())
}
