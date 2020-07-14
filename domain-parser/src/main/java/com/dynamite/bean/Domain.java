
package com.dynamite.bean;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class Domain implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -4851477811618820991L;

  private String domainName;

  private String date;

  private String source;

  public Domain(String domainName, String source) {
    super();
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate date = LocalDate.now().minusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String yyyyMMdd = sdf.format(Date.from(date.atStartOfDay(defaultZoneId).toInstant()));

    this.domainName = domainName;
    this.date = yyyyMMdd;
    this.setSource(source);
  }

  public Domain(String domainName, String date, String source) {
    super();

    this.domainName = domainName;
    this.date = date;
    this.setSource(source);
  }

  public Domain(String domainName) {
    super();
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate date = LocalDate.now().minusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String yyyyMMdd = sdf.format(Date.from(date.atStartOfDay(defaultZoneId).toInstant()));

    this.domainName = domainName;
    this.date = yyyyMMdd;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public String toString() {
    return "Domain [domainName=" + domainName + ", date=" + date + "]";
  }

}
