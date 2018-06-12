package nl.reinkrul.jmsgateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@SpringBootConfiguration
@EnableAsync
@SpringBootApplication
class Application {

    @Autowired
    private lateinit var engine: Engine

    @Bean
    fun init() = org.springframework.boot.CommandLineRunner {
        engine.start()
    }

    @Bean
    fun taskScheduler(): TaskScheduler =
            ThreadPoolTaskScheduler().apply {
                poolSize = 50
                isDaemon = false
            }

    @Bean
    fun configuration(environment: Environment): JmsConfiguration =
        JmsConfiguration(
                environment["jms.url"],
                if (environment.getProperty("jms.authentication.user") == null) null else authConfiguration(environment)
        )

    private fun authConfiguration(environment: Environment): JmsConfiguration.Authentication =
            JmsConfiguration.Authentication(environment["jms.authentication.user"], environment["jms.authentication.password"])
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}