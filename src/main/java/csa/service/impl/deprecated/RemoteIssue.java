package csa.service.impl.deprecated;

public class RemoteIssue {
  private String type;
  private String summary;
  private String project;
  private String priority;
  private String description;
  private RemoteCustomFieldValue[] customFieldValues;
  private String key;
  private String status;

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getSummary() {
    return summary;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getPriority() {
    return priority;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setCustomFieldValues(RemoteCustomFieldValue[] customFieldValues) {
    this.customFieldValues = customFieldValues;
  }

  public RemoteCustomFieldValue[] getCustomFieldValues() {
    return customFieldValues;
  }

  public String getKey() {
    return key;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
