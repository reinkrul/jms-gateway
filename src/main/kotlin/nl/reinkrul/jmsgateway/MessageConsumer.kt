/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway

interface MessageConsumer {

    @Throws(RecoverableException::class)
    fun consume(message: Message)

    val messagesConsumed: Int
}