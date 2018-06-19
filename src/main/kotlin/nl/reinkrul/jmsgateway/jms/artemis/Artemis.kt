/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway.jms.artemis

import nl.reinkrul.jmsgateway.JmsConfiguration
import nl.reinkrul.jmsgateway.jms.JmsAdapter
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient
import javax.jms.ConnectionFactory
import javax.jms.Queue

class ArtemisJmsAdapter : JmsAdapter {

    override fun createConnectionFactory(configuration: JmsConfiguration): ConnectionFactory =
            ActiveMQJMSClient.createConnectionFactory(configuration.url, null).also { connectionFactory ->
                configuration.authentication?.apply {
                    connectionFactory.user = user
                    connectionFactory.password = password
                }
                connectionFactory.disableFinalizeChecks()
            }

    override fun createQueue(name: String): Queue =
            ActiveMQJMSClient.createQueue(name)

}