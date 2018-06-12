package nl.reinkrul.jmsgateway

interface MessageConsumer {

    fun consume(message: Message)

    val messagesConsumed: Int
}