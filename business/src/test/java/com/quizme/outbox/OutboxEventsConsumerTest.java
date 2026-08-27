package com.quizme.outbox;

import com.quizme.IntegrationTest;
import com.quizme.email.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventsConsumerTest extends IntegrationTest {

    @Autowired
    private OutboxEventPublisher eventPublisher;

    @MockitoBean
    private EmailService emailService; // replaces the real bean with a Mockito mock in the context

    @Autowired
    private OutboxEventsConsumer consumer;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void init() throws MessagingException {
        MessageListenerContainer container = registry.getListenerContainer("outbox-events-consumer");
        // wait until the container has been assigned all partitions of the topic (1 partition here)
        // otherwise publishing might take place before container is assigned and therefore
        // the message won't be consumed because we have 'latest' message consumption policy.
        ContainerTestUtils.waitForAssignment(container, 1);

        doNothing().when(emailService).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void verifyHappyScenario() throws Exception {
        var signupEvent = new OutboxEvent(OutboxEventTypes.SIGN_UP, """
                {"email": "to@mail.com"}
                """);

        eventPublisher.publish(signupEvent);
        // timeout ensures the  test waits for consumer to finish consuming the msg
        verify(emailService, timeout(3000)).sendHtmlEmail(eq("to@mail.com"),
                any(),
                any());
    }
}