package notification.entity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class NotificationItemsConverter : AttributeConverter<List<NotificationItemData>, String> {
    private val mapper = jacksonObjectMapper()
    private val type = mapper.typeFactory.constructCollectionType(List::class.java, NotificationItemData::class.java)

    override fun convertToDatabaseColumn(attr: List<NotificationItemData>?): String =
        if (attr.isNullOrEmpty()) "[]" else mapper.writeValueAsString(attr)

    override fun convertToEntityAttribute(db: String?): List<NotificationItemData> =
        if (db.isNullOrBlank() || db == "[]") emptyList() else mapper.readValue(db, type)
}
