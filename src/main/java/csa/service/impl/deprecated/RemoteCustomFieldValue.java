package csa.service.impl.deprecated;

public class RemoteCustomFieldValue {
  private String customfieldId;
  private String[] values;

  public RemoteCustomFieldValue(String spCustomField, Object o, String[] strings) {

  }

  public String getCustomfieldId() {
    return customfieldId;
  }

  public void setCustomfieldId(String customfieldId) {
    this.customfieldId = customfieldId;
  }

  public String[] getValues() {
    return values;
  }
}
