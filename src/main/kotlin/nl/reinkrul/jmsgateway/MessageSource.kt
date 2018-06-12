package nl.reinkrul.jmsgateway


interface MessageSource {

    /**
     * Acquires a pending message from the source.
     */
    fun acquire(): Message

    /**
     * Marks a message as delivered.
     */
    fun markDelivered(message: Message)

    /**
     * Register a failed delivery for the message.
     */
    fun registerDeliveryFailed(message: Message)
}