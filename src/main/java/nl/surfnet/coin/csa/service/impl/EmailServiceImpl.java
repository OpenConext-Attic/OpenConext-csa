package nl.surfnet.coin.csa.service.impl;

import org.springframework.mail.SimpleMailMessage;

import nl.surfnet.coin.csa.service.EmailService;
import nl.surfnet.coin.csa.util.mail.Emailer;

public class EmailServiceImpl implements EmailService {

  private final String administrativeEmail;

  private final Emailer emailer;

  public EmailServiceImpl(String administrativeEmail, Emailer emailer) {
    this.administrativeEmail = administrativeEmail;
    this.emailer = emailer;
  }

  public void sendMail(String from, String subject, String body) {
    StringBuilder content = new StringBuilder("The following question was posted on self service portal:\n\n");
    content.append(body);

    SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
    simpleMailMessage.setFrom(from);
    simpleMailMessage.setTo(administrativeEmail);
    simpleMailMessage.setSubject(subject);
    simpleMailMessage.setText(content.toString());

    emailer.sendAsync(simpleMailMessage);
  }

}
