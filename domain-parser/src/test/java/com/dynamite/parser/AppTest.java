
package com.dynamite.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.dynamite.bean.Contact;

/**
 * Unit test for simple App.
 */
public class AppTest {

  private App app;

  private String url;

  @Before
  @Test
  public void setup() {
    app = new App();
    url = app.getImageUrl();
    assertNotNull(url);
  }

  @Test
  public void Test11_GetDomainsFromImage() {
    String result = app.getParsedText(url);
    List<String> domains = app.extractData(result);
    assertTrue(domains.size() > 0);
  }

  @Test
  public void test12_formatEmail() {
    String domainWithSpace = "agencemediaweb fr";
    assertEquals(app.checkFormat(domainWithSpace), "agencemediaweb.fr");

    String domainMissingDot = "agence-lconicfr";
    assertEquals(app.checkFormat(domainMissingDot), "agence-lconic.fr");

    String domainFormatOk = "adopteunapprenti-e.fr";
    assertEquals(app.checkFormat(domainFormatOk), "adopteunapprenti-e.fr");
  }

  @Test
  public void Test13_getRandomUA() {
    String ua = app.selectRandomUA();
    assertNotNull(ua);
  }

  @Test
  public void Test13_SendWhoisRequest() {
    List<Contact> contacts = app.whois("agencemediaweb.fr", false);
    assertTrue(contacts.size() > 0);
  }

  @Test
  public void Test14_FilterContacts() {

    Contact contact = new Contact("email", "domain", "contact", "adresse", "phone", "type", "country");
    Contact contact1 = new Contact("email2", "domain2", "contact2", "adresse2", "phone2", "type2", "country2");
    Contact contact2 = new Contact("email", "domain3", "contact3", "adresse3", "phone3", "type3", "country"); // Duplicates: should be deleted
    Contact contact3 = new Contact("email2", "domain2", "contact2", "adresse2", "phone2", "type2", "country2"); // Duplicates: should be deleted
    Contact contact4 = new Contact("email4", "domain3", "contact3", "adresse3", "phone3", "type3", "country");
    Contact contact5 = new Contact("emailsupport", "domain3", "contact3", "adresse3", "phone3", "type3", "country"); // word filter "support": should be deleted
    Contact contact6 = new Contact("emailsupport", "domain3", "contact3", "adresse3", "phone3", "type3", "country"); // word filter  "support":  should be deleted
    Contact contact7 = new Contact("emaildomainstest", "domain3", "contact3", "adresse3", "phone3", "type3", "country"); // word filter  "domains":should  be deleted
    Contact contact8 = new Contact("emailtest@key-systems.net", "domain3", "contact3", "adresse3", "phone3", "type3", "country"); // word filter "key-systems": should be deleted

    List<Contact> listContact = new ArrayList<Contact>();

    listContact.add(contact);
    listContact.add(contact1);
    listContact.add(contact2);
    listContact.add(contact3);
    listContact.add(contact4);
    listContact.add(contact5);
    listContact.add(contact6);
    listContact.add(contact7);
    listContact.add(contact8);

    List<Contact> listToRemove = app.filterContacts(listContact);
    listContact.removeAll(listToRemove);

    assertEquals(listContact.size(), 3);
  }

  @Test
  public void Test_realTest() {
    List<String> urls = new ArrayList<>();

    urls.add("http://51.77.149.226/20200619_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200621_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200622_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200623_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200624_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200625_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200626_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200627_CREA_fr.gif");
    urls.add("http://51.77.149.226/20200628_CREA_fr.gif");

    app.launch();
  }
}
