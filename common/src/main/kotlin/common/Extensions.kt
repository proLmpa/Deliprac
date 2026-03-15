package common

import java.util.Optional

fun <T> Optional<T>.orThrow(msg: String): T =
    orElseThrow { IllegalArgumentException(msg) }
