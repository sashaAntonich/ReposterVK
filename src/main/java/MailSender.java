import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

public class MailSender
{
    public static boolean SendMail(String toMail, String subject, String messageText){
        Email email = EmailBuilder.startingBlank()
                .from("course.project@bk.ru")
                .to(toMail)
                .withSubject(subject)
                .withPlainText(messageText)
                .buildEmail();

        Mailer mailer = MailerBuilder
                .withSMTPServer("smtp.mail.ru", 465, "course.project@bk.ru", "1de43Denis")
                .withTransportStrategy(TransportStrategy.SMTPS)
                .buildMailer();

        try {
            mailer.sendMail(email);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}