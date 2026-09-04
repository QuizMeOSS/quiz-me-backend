package com.quizme.outbox;

import com.quizme.IntegrationTest;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventsConsumerTest extends IntegrationTest {

    @Autowired
    private OutboxEventPublisher eventPublisher;

    @Autowired
    private OutboxEventsConsumer consumer;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void init() {
        MessageListenerContainer container = registry.getListenerContainer("outbox-events-consumer");
        // wait until the container has been assigned all partitions of the topic (1 partition here)
        // otherwise publishing might take place before container is assigned and therefore
        // the message won't be consumed because we have 'latest' message consumption policy.
        ContainerTestUtils.waitForAssignment(container, 1);
    }

    @Test
    void verifyHappyScenario() throws Exception {
        var signupEvent = new OutboxEvent(OutboxEventTypes.SIGN_UP, """
                {"email": "to@mail.com",
                "confirmationToken":"token"}
                """);

        eventPublisher.publish(signupEvent);
        // timeout ensures the  test waits for consumer to finish consuming the msg
        verify(emailService, timeout(3000)).sendHtmlEmail(eq("to@mail.com"),
                any(),
                any());
    }

    /**
     * A previous successfully handled message shouldn't be handled again
     */
    @Test
    void messageConsumptionIsIdempotent() throws Exception {
        var signupEvent = new OutboxEvent(OutboxEventTypes.SIGN_UP, """
                {"email": "to@mail.com",
                "confirmationToken":"token"}
                """);

        eventPublisher.publish(signupEvent);
        eventPublisher.publish(signupEvent);

        // after() waits the full duration before checking,
        // giving the consumer time to process both messages
        verify(emailService, after(3000).times(1)).sendHtmlEmail(eq("to@mail.com"),
                any(),
                any());
    }

    @Test
    void WHEN_eventProcessingFails_THEN_eventIsRetried() throws Exception {
        var signupEvent = new OutboxEvent(OutboxEventTypes.SIGN_UP, """
                {"email": "to@mail.com",
                "confirmationToken":"token"}
                """);
        // throw on first try and succeed on second try
        doThrow(new MessagingException())
                .doNothing()
                .when(emailService).sendHtmlEmail(any(), any(), any());

        eventPublisher.publish(signupEvent);
        // after() waits the full duration before checking,
        // giving the consumer time to retry
        verify(emailService, after(3000).times(2)).sendHtmlEmail(eq("to@mail.com"),
                any(),
                any());
    }
}