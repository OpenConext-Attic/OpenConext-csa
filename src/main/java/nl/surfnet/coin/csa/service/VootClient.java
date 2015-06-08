package nl.surfnet.coin.csa.service;

public interface VootClient {

  boolean hasAccess(String personId, String groupId);

}
