
package com.dynamite.parser.whois;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.dynamite.bean.Contact;
import com.dynamite.bean.Domain;
import com.dynamite.config.GetPropertyValues;
import com.dynamite.dao.ContactDAO;
import com.dynamite.dao.DomainDAO;

import helper.RequestHelper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class WhoisReader {

  private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  private static final GetPropertyValues props = new GetPropertyValues();

  private DomainDAO domainDAO = new DomainDAO();

  private ContactDAO contactDAO = new ContactDAO();

  private RequestHelper requestHelper = new RequestHelper();

  public List<Domain> domainToRetrieve = new ArrayList<>();

  public int nbRetrieveAttempts;

  public List<Contact> contactList;

  public static void main(String[] args) {
    WhoisReader whois = new WhoisReader();
    whois.launch();
  }

  public void launch() {
    try {
      while (domainDAO.countDomains() > 0) {
        List<Domain> domainsList = domainDAO.selectDomainsList();
        contactList = new ArrayList<Contact>();

        for (Domain domain : domainsList) {
          sleep(150);
          System.out.println("domain n°" + domainsList.indexOf(domain) + " sur " + domainsList.size());
          System.out.println("timeout domains : " + this.domainToRetrieve.size());
          System.out.println("unfilter contact number :" + contactList.size());

          switch (domain.getSource()) {
            case "AFNIC":
              // FR DOMAINS
              contactList.addAll(whoIsOnURLSCANFromAFNIC(domain, false));
              break;
            case "WHOISDS":
              // COM DOMAINS
              contactList.addAll(whoIsOnURLSCANFromWHOISDS(domain, false));
              break;
            default:
              break;
          }

        }

        retrieveFailedDomains();

        saveData(contactList);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public List<Contact> whoIsOnURLSCANFromWHOISDS(final Domain domain, final boolean retrieve) {
    List<Contact> contacts = new ArrayList<>();
    try {
      Map<String, String> listHeaders = new HashMap<>();
      listHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      listHeaders.put("Accept-Encoding", "gzip, deflate, br");
      listHeaders.put("Accept-Language", "en-US,en;q=0.9,fr-FR;q=0.8,fr;q=0.7,la;q=0.6");
      listHeaders.put("Connection", "keep-alive");
      listHeaders.put("Pragma", "no-cache");
      listHeaders.put("User-Agent", requestHelper.selectRandomUA());

      HttpResponse<String> response = Unirest.get("https://urlscan.io/domain/" + domain.getDomainName()).headers(listHeaders).asString();
      Document doc = Jsoup.parse(response.getBody());
      if (doc.getElementsByTag("pre").size() > 0) {
        String outerHtml = doc.outerHtml();
        String whoisDataFullBloc = outerHtml.split("<pre>")[1].split("</pre>")[0];

        Contact contact = extractWhoisDataFromWHOISDS(whoisDataFullBloc, domain.getDomainName());
        if (contact != null) {
          contacts.add(contact);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      if (!retrieve) {
        this.domainToRetrieve.add(domain);
      }
      e.printStackTrace();
    } catch (IllegalArgumentException | UnirestException e) {
      e.printStackTrace();
    }

    return contacts;
  }

  public Contact extractWhoisDataFromWHOISDS(final String bloc, final String domainName) {
    Contact contact = new Contact();

    String[] rows = bloc.split("\\n");
    Map<String, String> rowsKeyValue = new HashMap<>();

    for (String row : rows) {
      try {
        if (row != null && !row.isEmpty() && row.contains(":")) {
          String key = row.split(":", 2)[0].trim();
          String value = row.split(":", 2)[1].trim();
          if (rowsKeyValue.containsKey(key)) {
            String existingValue = rowsKeyValue.get(key);
            rowsKeyValue.replace(key, existingValue + " " + value);
          } else {
            rowsKeyValue.put(key, value);
          }
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        e.printStackTrace();
      }
    }

    String email = rowsKeyValue.get("Registrant Email");

    if (email != null && validateEmail(email)) {
      contact.setAddress(rowsKeyValue.get("Registrant Street"));
      contact.setContact(rowsKeyValue.get("Registrant Name"));
      contact.setCountry(rowsKeyValue.get("Registrant Country"));
      contact.setDomainName(domainName);
      contact.setEmail(rowsKeyValue.get("Registrant Email"));
      contact.setType("REGISTRANT");
      contact.setPhone(rowsKeyValue.get("Registrant Phone"));
    }

    return (contact.getEmail() == null) ? null : contact;
  }

  public List<Contact> whoIsOnURLSCANFromAFNIC(final Domain domain, final boolean retrieve) {
    List<Contact> contacts = new ArrayList<>();
    try {
      Map<String, String> listHeaders = new HashMap<>();
      listHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      listHeaders.put("Accept-Encoding", "gzip, deflate, br");
      listHeaders.put("Accept-Language", "en-US,en;q=0.9,fr-FR;q=0.8,fr;q=0.7,la;q=0.6");
      listHeaders.put("Connection", "keep-alive");
      listHeaders.put("Pragma", "no-cache");
      listHeaders.put("User-Agent", requestHelper.selectRandomUA());

      HttpResponse<String> response = Unirest.get("https://urlscan.io/domain/" + domain.getDomainName()).headers(listHeaders).asString();
      Document doc = Jsoup.parse(response.getBody());
      String outerHtml = doc.outerHtml();

      if (doc.getElementsByTag("pre").size() > 0) {

        String whoisDataFullBloc = outerHtml.split("<pre>")[1].split("</pre>")[0];
        String[] splittedBlocs = whoisDataFullBloc.split("\\n\\n");

        for (String bloc : splittedBlocs) {
          Contact contact = extractWhoisDataFromAFNIC(bloc, domain.getDomainName());
          if (contact != null) {
            contacts.add(contact);
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      if (!retrieve) {
        this.domainToRetrieve.add(domain);
      }
      e.printStackTrace();
    } catch (IllegalArgumentException | UnirestException e) {
      e.printStackTrace();
    }

    return contacts;
  }

  public Contact extractWhoisDataFromAFNIC(final String bloc, final String domainName) {
    Contact contact = new Contact();

    String[] rows = bloc.split("\\n");
    Map<String, String> rowsKeyValue = new HashMap<>();

    for (String row : rows) {
      try {
        if (row != null && !row.isEmpty() && row.contains(":")) {
          String key = row.split(":", 2)[0].trim();
          String value = row.split(":", 2)[1].trim();
          if (rowsKeyValue.containsKey(key)) {
            String existingValue = rowsKeyValue.get(key);
            rowsKeyValue.replace(key, existingValue + " " + value);
          } else {
            rowsKeyValue.put(key, value);
          }
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        e.printStackTrace();
      }
    }

    String type = rowsKeyValue.get("type");
    String email = rowsKeyValue.get("e-mail");
    boolean anonymous = (rowsKeyValue.get("anonymous") == "YES");
    if (("ORGANIZATION".equals(type) || "PERSON".equals(type)) && !anonymous && email != null) {
      contact.setAddress(rowsKeyValue.get("address"));
      contact.setContact(rowsKeyValue.get("contact"));
      contact.setCountry(rowsKeyValue.get("country"));
      contact.setDomainName(domainName);
      contact.setEmail(email);
      contact.setType(type);
      contact.setPhone(rowsKeyValue.get("phone"));
    }

    return (contact.getEmail() == null) ? null : contact;
  }

  public void retrieveFailedDomains() {
    int nbRetrieveAttempsMax;

    try {
      nbRetrieveAttempsMax = Integer.parseInt(props.getPropValue("numberAttempts"));
    } catch (NullPointerException | IOException e) {
      nbRetrieveAttempsMax = 0;
    }

    if (domainToRetrieve.size() > 0) {
      nbRetrieveAttempts = 0;
      while (nbRetrieveAttempts < nbRetrieveAttempsMax && domainToRetrieve.size() > 0) {
        List<Domain> hasSucceededDomains = new ArrayList();

        for (Domain domain : domainToRetrieve) {
          System.out.println("___________________________________________");
          System.out.println("attempt" + nbRetrieveAttempts);
          System.out.println("domain " + domainToRetrieve.indexOf(domain) + "of " + domainToRetrieve.size());
          System.out.println("timeoutDomains : " + domainToRetrieve.size());

          boolean hasSucceeded = false;
          try {
            sleep(150);
            switch (domain.getSource()) {
              case "AFNIC":
                this.contactList.addAll(whoIsOnURLSCANFromAFNIC(domain, true));
                break;
              case "WHOISDS":
                this.contactList.addAll(whoIsOnURLSCANFromWHOISDS(domain, true));
                break;
              default:
                break;
            }
            hasSucceeded = true;
          } catch (ArrayIndexOutOfBoundsException e) {
            hasSucceeded = false;
          }

          System.out.println("unfilter contact number :" + contactList.size());
          System.out.println("___________________________________________");

          if (hasSucceeded) {
            hasSucceededDomains.add(domain);
          }
        }

        domainToRetrieve.removeAll(hasSucceededDomains);
        nbRetrieveAttempts++;
      }

      System.out.println(domainToRetrieve.size());
    }
  }

  public static boolean validateEmail(String emailStr) {
    Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
    return matcher.find();
  }

  public void saveData(List<Contact> listContacts) {
    List<Contact> contactsToInsert = listContacts;
    List<Contact> duplicatesEmails = filterContacts(listContacts);
    contactsToInsert.removeAll(duplicatesEmails);

    System.out.print("Adding " + contactsToInsert.size() + " contacts");

    try {
      contactDAO.insertAll(contactsToInsert);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public List<Contact> filterContacts(List<Contact> listContacts) {
    ArrayList<String> wordToFilter = new ArrayList<>(Arrays.asList("host", "support", "domains", "domaine", "info", "key-systems", "tech", "nospam", "-dns", "dns-", "dns.", "clientele", "webmaster",
        "domain", "domeinen", "help@wordpress", "contact@", "registrar", "cctld", "nic", "dns@", "info@"));
    List<Contact> contactToRemove = new ArrayList<>();
    Function<Contact, ?> uniqueKey = el -> el.getEmail();
    Function<Contact, ?> notNullUniqueKey = el -> uniqueKey.apply(el) == null ? "" : uniqueKey.apply(el);
    contactToRemove.addAll(
        listContacts.stream().collect(Collectors.groupingBy(notNullUniqueKey)).values().stream().filter(matches -> matches.size() > 1).map(matches -> matches.get(0)).collect(Collectors.toList()));

    Iterator<String> wordIterator = wordToFilter.iterator();

    while (wordIterator.hasNext()) {
      String word = wordIterator.next();
      for (Contact contact : listContacts) {
        if (contact.getEmail().contains(word) && !contactToRemove.contains(contact)) {
          contactToRemove.add(contact);
        }
      }
    }

    return contactToRemove;
  }

  public void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}