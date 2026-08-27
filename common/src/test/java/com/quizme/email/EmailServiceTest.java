package com.quizme.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailServiceTest {

    private static final String FROM_EMAIL = "from@email.com";
    private static final String TO_EMAIL = "to@email.com";
    private static final String SUBJECT = "test email subject!";

    @RegisterExtension
    private static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            // no need to create an instance per test, just one per class is enough
            // we can use `reset()` to clear the messages after each method
            .withPerMethodLifecycle(false);


    private EmailService emailService;

    @BeforeEach
    void init() {
        JavaMailSenderImpl realSender = new JavaMailSenderImpl();
        realSender.setHost("localhost");
        realSender.setPort(GREEN_MAIL.getSmtp().getPort());
        GREEN_MAIL.reset(); // clears all received messages

        emailService = new EmailService(realSender, FROM_EMAIL);
    }

    @Test
    void sendHTMLBodyMail() throws MessagingException, IOException {
        var htmlBodyToSend = """
                <html>
                    <body>
                        <p>Hello</p>
                    </body>
                </html>
                """;
        emailService.sendHtmlEmail(TO_EMAIL, SUBJECT, htmlBodyToSend);

        MimeMessage[] received = GREEN_MAIL.getReceivedMessages();

        assertThat(received).hasSize(1);

        MimeMessage sentMessage = received[0];
        assertThat(sentMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
        assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo(TO_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo(SUBJECT);

        String receivedHtmlContent = extractHtmlContent(sentMessage);
        assertNotNull(receivedHtmlContent);
        assertEquals(normalize(htmlBodyToSend), normalize(receivedHtmlContent));
    }

    /**
     * Recursively walk the sent multipart email to find the HTML part.
     */
    @Nullable
    private String extractHtmlContent(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/html")) {
            return part.getContent().toString();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String result = extractHtmlContent(multipart.getBodyPart(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    String normalize(String html) {
        return html.replaceAll("\\s+", " ").trim();
    }
}