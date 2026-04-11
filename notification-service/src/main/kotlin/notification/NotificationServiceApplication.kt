package notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["notification", "common"])
class NotificationServiceApplication

fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}
