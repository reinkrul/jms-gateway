package nl.reinkrul.jmsgateway.jdbc

import nl.reinkrul.jmsgateway.Message
import nl.reinkrul.jmsgateway.MessageSource
import nl.reinkrul.jmsgateway.MessageStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.sql.DataSource

@Component
class JdbcMessageSource : MessageSource {

    private val log = LoggerFactory.getLogger(JdbcMessageSource::class.java)

    @Autowired
    private lateinit var dataSource: DataSource
    private lateinit var connection: Connection

    override fun acquire(): Message {
        log.info("Acquiring message.")
        while (true) {
            val message = query()
            if (message != null) {
                log.info("Acquired message: {}", message.id)
                return message
            } else {
                Thread.sleep(500)
            }
        }

    }

    private fun query(): Message? =
            // TODO: 'time_last_failed_attempt'
            connection.prepareStatement("SELECT * FROM jms_message " +
                    "WHERE status='${MessageStatus.PENDING}' " +
                    "AND retries <= max_retries " +
                    "LIMIT 1").use { statement ->
                statement.executeQuery().use { result ->
                    if (result.next())
                        parseMessage(result).also {
                            setStatus(it, MessageStatus.SENDING)
                        }
                    else
                        null
                }
            }

    override fun markDelivered(message: Message) {
        log.info("Marking message as delivered: {}", message.id)
        setStatus(message, MessageStatus.DELIVERED)
    }

    override fun registerDeliveryFailed(message: Message) {
        log.info("Registering a failed delivery: {}", message.id)
        connection.prepareStatement("UPDATE jms_message SET retries=retries + 1, status='${MessageStatus.PENDING}', last_change=NOW() WHERE id=? LIMIT 1").use { statement ->
            statement.setString(1, message.id.toString())
            if (statement.executeUpdate() != 1) {
                error("Expected update query to update exactly 1 record.")
            }
        }
    }

    @PostConstruct
    private fun initialize() {
        log.info("Acquiring database connection...")
        connection = dataSource.connection
    }

    @PreDestroy
    private fun release() {
        log.info("Releasing database connection...")
        if (!connection.isClosed) {
            connection.close()
        }
    }

    private fun setStatus(message: Message, status: MessageStatus) {
        log.debug("Setting status: message={}, status={}", message.id, status)
        connection.prepareStatement("UPDATE jms_message SET status=?, last_change=NOW() WHERE id=?").use { statement ->
            statement.setString(1, status.toString())
            statement.setString(2, message.id.toString())
            if (statement.executeUpdate() != 1) {
                error("Expected update query to update exactly 1 record.")
            }
        }
    }

    private fun parseMessage(result: ResultSet): Message {
        return Message(
                id = UUID.fromString(result.getString("id")),
                data = result.getString("data"),
                status = result.getString("status").let(MessageStatus::valueOf),
                queue = result.getString("queue")
        )
    }
}