package nl.reinkrul.jmsgateway.jms

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
import javax.jms.Session


@Component
class JmsMessageConsumer : MessageConsumer {

    private val log = LoggerFactory.getLogger(JmsMessageConsumer::class.java)
    private var numConsumed: Int = 0

    private lateinit var connection: Connection
    private lateinit var adapter: JmsAdapter
    private var session: Session? = null

    @Value("#{T(nl.reinkrul.jmsgateway.jms.JmsProvider).valueOf(\"\${jms.provider}\".toUpperCase())}")
    private lateinit var provider: JmsProvider
    @Autowired
    private lateinit var configuration: JmsConfiguration

    override fun consume(message: Message) {
        log.info("Consuming message: {}", message.id)
        with(assertSessionOpen()) {
            val messageToSend = createTextMessage().apply {
                text = message.data
            }
            adapter.createQueue(message.queue).let {
                createProducer(it)
            }.send(messageToSend)
        }
        numConsumed++
    }

    override val messagesConsumed: Int
        get() = numConsumed

    @PostConstruct
    private fun connect() {
        log.info("Connecting to JMS broker...")
        adapter = adapter(provider)
        connection = adapter.createConnectionFactory(configuration).createConnection()
        connection.start()
    }

    @PreDestroy
    private fun disconnect() {
        with(connection) {
            stop()
            close()
        }
    }

    private fun assertSessionOpen(): Session =
            if (session == null) {
                connection.createSession().also {
                    session = it
                }
            } else session!!
}