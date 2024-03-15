package dev.njabulo.reporter;

import dev.njabulo.spotter.Headline;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class NotificationSender {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSender.class);
    public static final int HEADLINE_MODERATE_SEVERITY = 4;

    @ConfigProperty(name = "my.mobile.number")
    String personalMobileNumber;
    @ConfigProperty(name = "zone.offset")
    String zoneOffset;

    @ConfigProperty(name = "twilio.account.sid")
    String accountSid;
    @ConfigProperty(name = "twilio.auth.token")
    String authToken;
    @ConfigProperty(name = "twilio.phone.number")
    String sourcePhoneNumber;

    public void auditHeadline(Headline warning) {
        logger.info("{} headline received with a severity of {} , effective {}", warning.category(), warning.severity(), warning.effectiveDate());
        boolean isWithinADay = isWithin24Hours(warning);
        if (warning.severity() <= HEADLINE_MODERATE_SEVERITY && isWithinADay) {
            //We care about this level of severity
            issueWarning(warning);
        }
    }

    private boolean isWithin24Hours(Headline warning) {
        ZoneOffset offset = ZoneOffset.of(zoneOffset);
        LocalDateTime now = LocalDateTime.now(offset);

        long hoursBetweenNowAndEffectiveWarningDate = now.until(warning.effectiveDate(), ChronoUnit.HOURS);
        return hoursBetweenNowAndEffectiveWarningDate <= 24;
    }

    private void issueWarning(Headline warning) {
        logger.info("Sending warning via SMS");
        String message = "Hey, just a heads up; in under 24 hrs, the weather may change. " +
                "[" + warning.headline() + " - " + warning.category() + "]";
        String messageSid = new TwilioSMS(accountSid, authToken).sendMessage(message, sourcePhoneNumber, personalMobileNumber);
        logger.info("Message sent. SID: {}", messageSid);
    }
}
