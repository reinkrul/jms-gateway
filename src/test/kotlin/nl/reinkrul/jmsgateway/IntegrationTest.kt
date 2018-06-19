/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway

import nl.reinkrul.jmsgateway.jdbc.parseMessage
import org.junit.Ignore
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [IntegrationTestConfiguration::class, Application::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var messageConsumer: MessageConsumer

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `send a single message`() {
        dataSource.connection.use {
            insert(it)
        }
        Thread.sleep(1000)
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
        } while (messageConsumer.messagesConsumed < numberOfMessages)
        assertEquals(numberOfMessages, messageConsumer.messagesConsumed)
        dataSource.connection.use {
            assertStatus(it, MessageStatus.DELIVERED)
        }
    }

    @Test
    @Ignore
    fun `failed message - unknown queue`() {
        val id = dataSource.connection.use {
            insert(it, "non-existing-queue")
        }
        Thread.sleep(2000)
        dataSource.connection.use {
            assertStatus(it, id, MessageStatus.PENDING)
        }
    }

    @Test
    fun `failed message - disconnected session`() {
        // Make sure the session is open
        val id1 = dataSource.connection.use {
            insert(it, "test")
        }
        Thread.sleep(1000)
        dataSource.connection.use {
            assertStatus(it, id1, MessageStatus.DELIVERED)
        }

        // Now stop the broker, causing the connections to disconnect
        activemq.stop()

        // Now insert a message, and assert it isn't delivered
        val id2 = dataSource.connection.use {
            insert(it, "test")
        }
        Thread.sleep(1000)
        dataSource.connection.use {
            assertStatus(it, id2, MessageStatus.PENDING)
        }

        // Now start the broker, insert message and assert it's delivered
        activemq.start()
        val id3 = dataSource.connection.use {
            insert(it, "test")
        }
        Thread.sleep(1000)
        dataSource.connection.use {
            assertStatus(it, id3, MessageStatus.DELIVERED)
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

    private fun assertStatus(connection: Connection, messageId: String, vararg expected: MessageStatus) {
        connection.createStatement().use {
            it.executeQuery("SELECT * FROM jms_message WHERE id='$messageId'").use {
                if (it.next()) {
                    assertStatus(arrayOf(*expected), parseMessage(it))
                } else {
                    fail("Message not found: $messageId")
                }
            }
        }
    }

    private fun assertStatus(connection: Connection, vararg expected: MessageStatus) {
        connection.createStatement().use {
            it.executeQuery("SELECT * FROM jms_message").use {
                while (it.next()) {
                    assertStatus(arrayOf(*expected), parseMessage(it))
                }
            }
        }
    }

    private fun assertStatus(expected: Array<MessageStatus>, actual: Message) {
        assertTrue("Expected message ${actual.id} to have status ${expected.asList()}, but was ${actual.status}", { expected.contains(actual.status) })
    }
}
