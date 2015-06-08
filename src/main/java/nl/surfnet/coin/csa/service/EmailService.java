package nl.surfnet.coin.csa.service;

public interface EmailService {

  void sendMail(String from, String subject, String body);

}
