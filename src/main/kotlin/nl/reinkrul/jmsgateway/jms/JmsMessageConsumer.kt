/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway.jms

import nl.reinkrul.jmsgateway.RecoverableException
import nl.reinkrul.jmsgateway.JmsConfiguration
import nl.reinkrul.jmsgateway.Message
import nl.reinkrul.jmsgateway.MessageConsumer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Queue
import javax.jms.Session
import javax.jms.TextMessage


@Component
class JmsMessageConsumer : MessageConsumer {

    private val log = LoggerFactory.getLogger(JmsMessageConsumer::class.java)
    private var numConsumed: Int = 0

    private lateinit var adapter: JmsAdapter
    private var connection: Connection? = null
    private var session: Session? = null
    private val queues: MutableMap<String, ResolvedQueue> = mutableMapOf()

    @Value("#{T(nl.reinkrul.jmsgateway.jms.JmsProvider).valueOf(\"\${jms.provider}\".toUpperCase())}")
    private lateinit var provider: JmsProvider
    @Autowired
    private lateinit var configuration: JmsConfiguration

    override fun consume(message: Message) {
        log.info("Consuming message: {}", message.id)
        send(message.queue) { jmsMessage, producer ->
            jmsMessage.text = message.data
            producer.send(jmsMessage)
            numConsumed++
        }
    }

    override val messagesConsumed: Int
        get() = numConsumed

    @PostConstruct
    private fun initialize() {
        try {
            connect()
        } catch (e: Exception) {
            log.warn("Unable to connect during startup, connection will be established later.", e)
        }
    }

    private fun connect(): Connection {
        log.info("Connecting to JMS broker...")
        adapter = adapter(provider)
        return adapter.createConnectionFactory(configuration).createConnection().apply {
            start()
            connection = this
        }
    }

    @PreDestroy
    private fun disconnect() {
        queues.clear()
        try {
            session?.close()
        } catch (e: Exception) {
            log.debug("Could not close session.", e)
        }
        session = null
        connection?.apply {
            try {
                stop()
            } catch (e: Exception) {
                log.debug("Could not stop connection.", e)
            }
            try {
                close()
            } catch (e: Exception) {
                log.debug("Could not close connection.", e)
            }
        }
        connection = null
    }

    private fun send(queue: String, block: (message: TextMessage, producer: MessageProducer) -> Unit) {
        fun prepare(): Pair<TextMessage, MessageProducer> {
            for (attempt in 1..3) {
                try {
                    val (session, message) = with(assertSessionOpen()) {
                        this to createTextMessage()!!
                    }
                    val resolvedQueue = queues.computeIfAbsent(queue, {
                        adapter.createQueue(queue).let {
                            ResolvedQueue(it, session.createProducer(it))
                        }
                    })
                    return message to resolvedQueue.producer
                } catch (e: Exception) {
                    log.warn("Could not create message at attempt $attempt (is the session closed?), retrying...", e)
                    disconnect()
                }
            }
            throw JmsException("Could not acquire session.")
        }
        with(prepare()) { block(first, second) }
    }

    private fun assertConnectionOpen(): Connection =
            if (connection == null)
                connect()
            else
                connection!!


    private fun assertSessionOpen(): Session =
            if (session == null)
                assertConnectionOpen().createSession().also {
                    session = it
                }
            else
                session!!

    private data class ResolvedQueue(val queue: Queue, val producer: MessageProducer)
}

private class JmsException(message: String, cause: Throwable? = null) : RecoverableException(message, cause)