/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway


import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import java.util.UUID

class EngineTest {

    @Test
    fun `failed delivery is registered`() {
        val message = Message(UUID.randomUUID(), "", MessageStatus.PENDING, "test", 0)
        val messageSource = mock<MessageSource>()
        whenever(messageSource.acquire()).thenReturn(message)
        val messageConsumer = mock<MessageConsumer>()
        whenever(messageConsumer.consume(any())).thenThrow(IllegalStateException())

        val engine = Engine(messageSource, messageConsumer)
        val thread = Thread({ engine.start() })
        thread.start()
        thread.interrupt()
        thread.join(1000)

        verify(messageSource).registerDeliveryFailed(message)
    }
}