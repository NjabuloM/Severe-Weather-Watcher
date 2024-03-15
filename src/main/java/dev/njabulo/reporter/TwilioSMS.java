package dev.njabulo.reporter;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.twilio.Twilio;
import com.twilio.converter.Promoter;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioSMS {

    public TwilioSMS(String accountSid, String authToken) {
        Twilio.init(accountSid, authToken);
    }

    public String sendMessage(String messageText, String sourcePhoneNumber, String destinationMobileNumber) {
        Message message = Message.creator(
            new com.twilio.type.PhoneNumber(destinationMobileNumber),
            new com.twilio.type.PhoneNumber(sourcePhoneNumber),
            messageText
        ).create();

        return message.getSid();
    }
}
