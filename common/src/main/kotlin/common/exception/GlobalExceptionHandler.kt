package common.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "not-found", ex.message!!, req)

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "forbidden", ex.message!!, req)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, "conflict", ex.message!!, req)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "bad-request", ex.message ?: "Bad request", req)

    private fun problem(status: HttpStatus, type: String, detail: String, req: HttpServletRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.type = URI.create("https://baemin.com/problems/$type")
            this.instance = URI.create(req.requestURI)
        }
}
