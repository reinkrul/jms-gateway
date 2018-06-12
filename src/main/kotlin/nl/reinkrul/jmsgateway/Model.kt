package nl.reinkrul.jmsgateway

import java.util.UUID

enum class MessageStatus {
    PENDING,
    SENDING,
    DELIVERED,
    FAILED
}

class Message(val id: UUID, val data: String, val status: MessageStatus, val queue: String)

data class JmsConfiguration(val url: String, val authentication: Authentication?) {

    data class Authentication(val user: String, val password: String)
}

