package csa.model;

public class LicenseContactPerson {

  private final String name;
  private final String email;
  private final String phone;
  private final String idpEntityId;

  public LicenseContactPerson(String name, String email, String phone, String idpEntityId) {
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.idpEntityId = idpEntityId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getIdpEntityId() {
    return idpEntityId;
  }
}
