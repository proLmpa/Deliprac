package store

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["store", "common"])
class StoreServiceApplication

fun main(args: Array<String>) {
    runApplication<StoreServiceApplication>(*args)
}
