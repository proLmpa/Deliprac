package store

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["store", "common"])
class StoreServiceApplication

fun main(args: Array<String>) {
    runApplication<StoreServiceApplication>(*args)
}
