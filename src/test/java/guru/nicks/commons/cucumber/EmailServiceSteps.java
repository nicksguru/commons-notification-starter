package guru.nicks.commons.cucumber;

import guru.nicks.commons.notification.impl.EmailServiceImpl;
import guru.nicks.commons.notification.service.EmailService;
import guru.nicks.commons.service.FreemarkerTemplateService;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Builder;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmailServiceSteps {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private FreemarkerTemplateService templateService;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private MimeMessageHelper messageHelper;
    private AutoCloseable closeableMocks;

    private EmailService emailService;
    private Map<String, Object> templateContext;
    private String renderedTemplate;
    private String templateName;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        templateContext = new HashMap<>();
        renderedTemplate = "<p>Rendered template content</p>";
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public TemplateContext createTemplateContext(Map<String, String> entry) {
        return TemplateContext.builder()
                .key(entry.get("key"))
                .value(entry.get("value"))
                .build();
    }

    @Given("an email service is configured")
    public void anEmailServiceIsConfigured() {
        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        emailService = new EmailServiceImpl(mailSender, templateService);
    }

    @Given("an email service is configured with failing message helper")
    public void anEmailServiceIsConfiguredWithFailingMessageHelper() throws MessagingException {
        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        doThrow(new MessagingException("Failed to set message properties"))
                .when(messageHelper).setFrom(anyString());

        emailService = new EmailServiceImpl(mailSender, templateService);
    }

    @Given("a template {string} exists with context")
    public void aTemplateExistsWithContext(String templateName, java.util.List<TemplateContext> contexts) {
        for (TemplateContext context : contexts) {
            templateContext.put(context.getKey(), context.getValue());
        }

        when(templateService.render(eq(templateName), any()))
                .thenReturn(renderedTemplate);
    }

    @When("an HTML email is sent from {string} to {string} with subject {string} and body {string}")
    public void anHtmlEmailIsSentFromToWithSubjectAndBody(String from, String to, String subject, String body) {
        emailService.sendHtml(from, to, subject, body);
    }

    @When("an HTML email with template is sent from {string} to {string} with subject {string} and template {string}")
    public void anHtmlEmailWithTemplateIsSentFromToWithSubjectAndTemplate(
            String from, String to, String subject, String templateName) {
        this.templateName = templateName;
        emailService.sendHtmlWithTemplate(from, to, subject, templateName, templateContext);
    }

    @Then("the template should be rendered with correct name and parameters")
    public void theTemplateShouldBeRenderedWithCorrectNameAndParameters() {
        verify(templateService).render(templateName, templateContext);
    }

    @Then("the email should be sent")
    public void theEmailShouldBeSent() {
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Value
    @Builder
    public static class TemplateContext {

        String key;
        String value;

    }

}
