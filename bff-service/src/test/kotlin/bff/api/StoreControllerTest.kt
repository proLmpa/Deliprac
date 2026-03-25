package bff.api

import bff.client.StoreClient
import bff.config.SecurityConfig
import bff.dto.FindStoreRequest
import bff.dto.ListStoreRequest
import bff.dto.StoreResponse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(StoreController::class)
@Import(SecurityConfig::class)
class StoreControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var storeClient: StoreClient

    private val token = "Bearer test-token"

    private val sampleStore = StoreResponse(
        id = 1L, name = "Store A", address = "Seoul", phone = "010-1234-5678",
        content = "desc", status = "ACTIVE", storePictureUrl = null,
        productCreatedTime = 0L, openedTime = 0L, closedTime = 0L, closedDays = "",
        averageRating = 4.5, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun `POST stores list - 200 with store list`() {
        val request = ListStoreRequest("RATING")
        given(storeClient.listStores(request, token)).willReturn(listOf(sampleStore))

        mockMvc.perform(
            post("/api/stores/list")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sortBy":"RATING"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Store A"))
    }

    @Test
    fun `POST stores find - 200 with store`() {
        val request = FindStoreRequest(1L)
        given(storeClient.findStore(request, token)).willReturn(sampleStore)

        mockMvc.perform(
            post("/api/stores/find")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":1}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.averageRating").value(4.5))
    }

    @Test
    fun `POST stores mine - 200 with owner store list`() {
        given(storeClient.myStores(token)).willReturn(listOf(sampleStore))

        mockMvc.perform(
            post("/api/stores/mine")
                .header("Authorization", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
    }
}
