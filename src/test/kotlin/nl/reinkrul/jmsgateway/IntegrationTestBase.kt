/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.reinkrul.jmsgateway

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

abstract class IntegrationTestBase {

    @Autowired
    protected val activemq: EmbeddedActiveMQ = EmbeddedActiveMQ()
}
