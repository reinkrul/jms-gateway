/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class Engine(@Autowired private val messageSource: MessageSource,
             @Autowired private val messageConsumer: MessageConsumer) {

    private val log = LoggerFactory.getLogger(Engine::class.java)


    private var running: Boolean = true

    @Async
    fun start() {
        log.info("Starting engine...")
        try {
            while (running)
                try {
                    consume()
                    if (Thread.currentThread().isInterrupted)
                        throw InterruptedException()
                } catch (e: InterruptedException) {
                    log.debug("Thread was interrupted, stopping...")
                    running = false
                }
        } catch (e: Exception) {
            log.error("An unexpected error occured, application will exit.", e)
            Runtime.getRuntime().exit(1)
        }
    }

    private fun consume() {
        val message = try {
            messageSource.acquire()
        } catch (e: RecoverableException) {
            log.error("Recoverable error occurred while acquiring message.", e)
            return
        }
        log.info("Processing message: {} (retries: {})", message.id, message.retries)
        try {
            messageConsumer.consume(message)
        } catch (e: RecoverableException) {
            log.error("Recoverable error occurred while processing message: {}", message.id, e)
            messageSource.registerDeliveryFailed(message)
            return
        }
        messageSource.markDelivered(message)
        log.info("Message processed: {}", message.id)
    }
}