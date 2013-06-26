package nl.surfnet.coin.csa.domain;

public class MappingEntry {

  private String key;
  private String value;

  public MappingEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MappingEntry that = (MappingEntry) o;

    if (!key.equals(that.key)) return false;
    if (value != null ? !value.equals(that.value) : that.value != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
