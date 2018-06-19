/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway.jms

import nl.reinkrul.jmsgateway.JmsConfiguration
import nl.reinkrul.jmsgateway.jms.artemis.ArtemisJmsAdapter
import javax.jms.ConnectionFactory
import javax.jms.Queue

interface JmsAdapter {

    fun createConnectionFactory(configuration: JmsConfiguration): ConnectionFactory
    fun createQueue(name: String): Queue
}

fun adapter(provider: JmsProvider): JmsAdapter =
        when (provider) {
            JmsProvider.ARTEMIS -> ArtemisJmsAdapter()
        }

enum class JmsProvider {
    ARTEMIS
}