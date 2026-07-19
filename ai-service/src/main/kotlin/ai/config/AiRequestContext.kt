package ai.config

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class AiRequestContext {
    var jwt: String? = null
}
