package store.entity.product

import common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "products")
open class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var price: Long,

    @Column(name = "product_picture_url", length = 500)
    var productPictureUrl: String?,

    @Column(nullable = false)
    var popularity: Long,

    @Column(nullable = false)
    var status: Boolean,

) : BaseEntity()
