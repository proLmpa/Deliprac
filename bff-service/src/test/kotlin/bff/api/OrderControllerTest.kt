package bff.api

import bff.client.AddCartItemRequest
import bff.client.NotificationClient
import bff.client.OrderClient
import bff.client.StoreClient
import bff.config.SecurityConfig
import bff.dto.AddToCartRequest
import bff.dto.CartProductResponse
import bff.dto.CartResponse
import bff.dto.CheckoutRequest
import bff.dto.FindProductRequest
import bff.dto.FindStoreRequest
import bff.dto.OrderResponse
import bff.dto.ProductResponse
import bff.dto.StoreResponse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var orderClient: OrderClient
    @MockitoBean private lateinit var storeClient: StoreClient
    @MockitoBean private lateinit var notificationClient: NotificationClient

    private val token = "Bearer test-token"

    private val sampleCart = CartResponse(
        id = 50L, storeId = 10L, isOrdered = false,
        items = listOf(CartProductResponse(id = 1L, productId = 100L, quantity = 2L, unitPrice = 8000L)),
        totalPrice = 16000L
    )

    private val sampleOrder = OrderResponse(
        id = 200L, storeId = 10L, totalPrice = 16000L,
        status = "PENDING", createdAt = 0L, updatedAt = 0L
    )

    private val sampleStore = StoreResponse(
        id = 10L, userId = 99L, name = "Test Store", address = "Seoul", phone = "02-1234",
        content = "Good food", status = "ACTIVE", storePictureUrl = null,
        productCreatedTime = 0L, openedTime = 0L, closedTime = 0L, closedDays = "",
        averageRating = 0.0, createdAt = 0L, updatedAt = 0L
    )

    private val sampleProduct = ProductResponse(
        id = 100L, storeId = 10L, name = "Burger", description = "desc",
        price = 8000L, productPictureUrl = null, popularity = 0L,
        status = true, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun `POST carts - fetches price from store-service then calls order-service`() {
        val bffRequest = AddToCartRequest(productId = 100L, storeId = 10L, quantity = 2L)
        val findRequest = FindProductRequest(storeId = 10L, productId = 100L)
        val orderRequest = AddCartItemRequest(productId = 100L, storeId = 10L, unitPrice = 8000L, quantity = 2L)

        given(storeClient.findProduct(findRequest, token)).willReturn(sampleProduct)
        given(orderClient.addCartItem(orderRequest, token)).willReturn(sampleCart)

        mockMvc.perform(
            post("/api/carts")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":100,"storeId":10,"quantity":2}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(50))
            .andExpect(jsonPath("$.totalPrice").value(16000))

        verify(storeClient).findProduct(findRequest, token)
        verify(orderClient).addCartItem(orderRequest, token)
    }

    @Test
    fun `POST carts me - 200 with active cart`() {
        given(orderClient.getMyCart(token)).willReturn(sampleCart)

        mockMvc.perform(
            post("/api/carts/me")
                .header("Authorization", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(50))
            .andExpect(jsonPath("$.items[0].unitPrice").value(8000))
    }

    @Test
    fun `PUT checkout - 200 with pending order and triggers notification`() {
        given(orderClient.checkout(CheckoutRequest(50L), token)).willReturn(sampleOrder)
        given(storeClient.findStore(FindStoreRequest(10L), token)).willReturn(sampleStore)

        mockMvc.perform(
            put("/api/carts/checkout")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cartId":50}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalPrice").value(16000))
    }

    @Test
    fun `POST users me orders - 200 with order list`() {
        given(orderClient.myOrders(token)).willReturn(listOf(sampleOrder))

        mockMvc.perform(
            post("/api/users/me/orders")
                .header("Authorization", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(200))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }
}
