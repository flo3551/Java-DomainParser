
package com.dynamite.bean;

import java.io.Serializable;
import java.util.UUID;

public class Contact implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -8246581318630620782L;

  private String domainName;

  private String type;

  private String country;

  private String contact;

  private String email;

  private String phone;

  private String address;

  private boolean isShopify;
  
  private String uuid;

  public Contact() {
    super();
    this.setUuid(UUID.randomUUID().toString().replace("-", ""));
  }

  public Contact(String email, String domainName, String contact, String address, String phone, String type, String country, boolean isShopify) {
    super();
    this.email = email;
    this.domainName = domainName;
    this.contact = contact;
    this.address = address;
    this.phone = phone;
    this.type = type;
    this.country = country;
    this.isShopify = isShopify;
    this.setUuid(UUID.randomUUID().toString().replace("-", ""));
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    this.domainName = domainName;
  }

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public boolean isShopify() {
    return isShopify;
  }

  public void setShopify(boolean isShopify) {
    this.isShopify = isShopify;
  }

  @Override
  public String toString() {
    return "Contact [domainName=" + domainName + ", type=" + type + ", country=" + country + ", contact=" + contact + ", email=" + email + ", phone=" + phone + ", address=" + address + "]";
  }
}
