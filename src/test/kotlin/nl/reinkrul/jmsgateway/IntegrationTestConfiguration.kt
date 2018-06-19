/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
@Order(1)
class IntegrationTestConfiguration {

    private lateinit var activemq: EmbeddedActiveMQ

    init {
        activemq = EmbeddedActiveMQ().start()
    }

    @Bean
    fun activemq(): EmbeddedActiveMQ =
            activemq
}