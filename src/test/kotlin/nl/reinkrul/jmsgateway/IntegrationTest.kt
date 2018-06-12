package nl.reinkrul.jmsgateway

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import java.sql.Connection
import java.sql.ResultSet
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTest {

    @Autowired
    private lateinit var messageConsumer: MessageConsumer

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `send a single message`() {
        dataSource.connection.use {
            insert(it)
        }
        Thread.sleep(2000)
        assertEquals(1, messageConsumer.messagesConsumed)
        dataSource.connection.use {
            assertStatus(it, MessageStatus.DELIVERED)
        }
    }

    @Test
    fun `send a batch of messages`() {
        val numberOfMessages = 500
        dataSource.connection.use {
            it.autoCommit = false
            for (i in 1..numberOfMessages) {
                insert(it)
            }
            it.commit()
        }
        val maxWaitTime = Duration.ofSeconds(30)
        val startTime = LocalDateTime.now()
        do {
            Thread.sleep(100)
            if (startTime.plus(maxWaitTime).isBefore(LocalDateTime.now())) {
                fail("Waited for $maxWaitTime to receive $numberOfMessages messages, but received just ${messageConsumer.messagesConsumed}")
            }
        } while(messageConsumer.messagesConsumed < numberOfMessages)
        assertEquals(numberOfMessages, messageConsumer.messagesConsumed)
        dataSource.connection.use {
            assertStatus(it, MessageStatus.DELIVERED)
        }
    }

    @Test
    fun `failed message - unknown queue`() {
        val id = dataSource.connection.use {
            insert(it, "non-existing-queue")
        }
        Thread.sleep(2000)
        dataSource.connection.use {
            assertStatus(it, MessageStatus.PENDING, id)
        }
    }

    private fun insert(connection: Connection, queue: String = "jms.queue.test"): String {
        val id = UUID.randomUUID().toString()
        connection.prepareStatement("INSERT INTO jms_message (id, status, data, queue) VALUES (?, ?, ?, ?)").use {
            it.setString(1, id)
            it.setString(2, MessageStatus.PENDING.toString())
            it.setString(3, "Hello, World!")
            it.setString(4, queue)
            it.executeUpdate()
        }
        return id
    }

    private fun assertStatus(connection: Connection, expected: MessageStatus, messageId: String) {
        connection.createStatement().use {
            it.executeQuery("SELECT * FROM jms_message WHERE id='$messageId'").use {
                if (it.next()) {
                    assertStatus(expected, it)
                } else {
                    fail("Message not found: $messageId")
                }
            }
        }
    }

    private fun assertStatus(connection: Connection, expected: MessageStatus) {
        connection.createStatement().use {
            it.executeQuery("SELECT id, status FROM jms_message").use {
                while (it.next()) {
                    assertStatus(expected, it)
                }
            }
        }
    }

    private fun assertStatus(expected: MessageStatus, it: ResultSet) {
        assertEquals(expected, MessageStatus.valueOf(it.getString("status")), "Expected message ${it.getString("id")} to have status $expected")
    }
}
