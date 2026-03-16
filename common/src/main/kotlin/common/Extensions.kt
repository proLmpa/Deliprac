package common

import common.exception.NotFoundException
import java.util.Optional

fun <T> Optional<T>.orThrow(msg: String): T =
    orElseThrow { NotFoundException(msg) }
