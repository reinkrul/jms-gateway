/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway


interface MessageSource {

    /**
     * Acquires a pending message from the source.
     */
    @Throws(RecoverableException::class)
    fun acquire(): Message

    /**
     * Marks a message as delivered.
     */
    @Throws(RecoverableException::class)
    fun markDelivered(message: Message)

    /**
     * Register a failed delivery for the message.
     */
    @Throws(RecoverableException::class)
    fun registerDeliveryFailed(message: Message)
}