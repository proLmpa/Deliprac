package common.exception

import org.springframework.http.HttpStatus

abstract class BaeminException (
    errorMessage: String,
): RuntimeException(errorMessage) {
    abstract val status: HttpStatus
    abstract val errorType: String

}

class ConflictException(message: String) : BaeminException(message) {
    override val status: HttpStatus = HttpStatus.CONFLICT
    override val errorType: String = "conflict"
}

class ForbiddenException(message: String) : BaeminException(message) {
    override val status: HttpStatus = HttpStatus.FORBIDDEN
    override val errorType: String = "forbidden"
}

class NotFoundException(message: String) : BaeminException(message) {
    override val status: HttpStatus = HttpStatus.NOT_FOUND
    override val errorType: String = "not-found"
}