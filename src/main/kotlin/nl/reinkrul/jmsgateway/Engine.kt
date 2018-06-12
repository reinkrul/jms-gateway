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
        while (running)
            try {
                consume()
                if (Thread.currentThread().isInterrupted)
                    throw InterruptedException()
            } catch (e: InterruptedException) {
                log.debug("Thread was interrupted, stopping...")
                running = false
            }
    }

    private fun consume() {
        val message = messageSource.acquire()
        log.info("Processing message: {}", message.id)
        try {
            messageConsumer.consume(message)
        } catch (e: Exception) {
            log.error("Error while processing message: {}", message.id, e)
            messageSource.registerDeliveryFailed(message)
            return
        }
        messageSource.markDelivered(message)
        log.info("Message processed: {}", message.id)
    }
}