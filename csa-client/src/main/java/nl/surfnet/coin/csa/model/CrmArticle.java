package nl.surfnet.coin.csa.model;

public class CrmArticle {

  private String guid;

  private String appleAppStoreUrl;
  private String androidPlayStoreUrl;

  public String getAndroidPlayStoreUrl() {
    return androidPlayStoreUrl;
  }

  public void setAndroidPlayStoreUrl(String androidPlayStoreUrl) {
    this.androidPlayStoreUrl = androidPlayStoreUrl;
  }

  public String getAppleAppStoreUrl() {
    return appleAppStoreUrl;
  }

  public void setAppleAppStoreUrl(String appleAppStoreUrl) {
    this.appleAppStoreUrl = appleAppStoreUrl;
  }
}
