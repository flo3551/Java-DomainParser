
package com.dynamite.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.dynamite.bean.Contact;
import com.dynamite.dao.ContactDAO;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class App {

  private static final String URL_FIRST_PART = "https://www.afnic.fr/data/divers/public/publication-quotidienne/";

  private static final String URL_SECOND_PART = "_CREA_fr.gif";

  private static final String USERAGENT_FILEPATH = "./src/main/ressources/ua.txt";

  private static final String API_KEY = "4f4f70b8f388957";

  private static final String API_GET_ENDPOINT = "https://api.ocr.space/parse/imageurl?";

  private static final String DATA_INTRO_DELIMITER = "\\*BOF";

  private static final String DATA__DELIMITER = "\\r\\n";

  public List<String> domainToRetrieve = new ArrayList<>();

  public List<Contact> listContact = new ArrayList<>();

  public int nbRetrieveAttempts = 0;

  private ContactDAO contactDAO = new ContactDAO();

  public int indexUrl;

  public static void main(String[] args) {
    App app = new App();
    app.launch();
  }

  public void launch() {
    String url = this.getImageUrl();
    String parsedText = getParsedText(url);
    List<String> domainList = extractData(parsedText);

    listContact = new ArrayList<>();
    int index = 0;

    for (String domain : domainList) {
      System.out.println("___________________________________________");
      System.out.println("domain " + index + "of " + domainList.size());
      System.out.println("timeoutDomains : " + domainToRetrieve.size());
      sleep(3000);
      listContact.addAll(whois(domain, false));

      System.out.println("unfilter contact number :" + listContact.size());
      System.out.println("___________________________________________");
      index++;
    }

    retrieveFailedDomains();

    List<Contact> duplicatesEmails = filterContacts(listContact);
    listContact.removeAll(duplicatesEmails);

    saveData(listContact);
  }

  public void retrieveFailedDomains() {
    int initialNumberToRetrieve = domainToRetrieve.size();
    if (domainToRetrieve.size() > 0) {
      nbRetrieveAttempts = 0;
      while (nbRetrieveAttempts < 150 && domainToRetrieve.size() > 0) {
        int leftToRetrieve = domainToRetrieve.size();

        List<String> hasSucceededDomains = new ArrayList<String>();
        for (String domain : domainToRetrieve) {
          System.out.println("___________________________________________");
          System.out.println("attempt" + nbRetrieveAttempts);
          System.out.println("domain " + domainToRetrieve.indexOf(domain) + "of " + domainToRetrieve.size());
          System.out.println("timeoutDomains : " + domainToRetrieve.size());
          boolean hasSucceeded = false;
          try {
            sleep(3000);
            this.listContact.addAll(whois(domain, true));
            hasSucceeded = true;
          } catch (ArrayIndexOutOfBoundsException e) {
            hasSucceeded = false;
          }
          System.out.println("unfilter contact number :" + listContact.size());
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

  public List<Contact> filterContacts(List<Contact> listContacts) {
    ArrayList<String> wordToFilter = new ArrayList<>(Arrays.asList("host", "support", "domains", "domaine", "info", "key-systems", "tech", "nospam", "-dns", "dns-", "dns.", "clientele", "webmaster",
        "domain", "domeinen", "help@wordpress", "contact@", "registrar", "cctld", "nic", "dns@"));
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

  public String getImageUrl() {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate date = LocalDate.now().minusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String yyyyMMdd = sdf.format(Date.from(date.atStartOfDay(defaultZoneId).toInstant()));

    return URL_FIRST_PART + yyyyMMdd + URL_SECOND_PART;
  }

  public String getParsedText(final String url) {
    String requestUrl = API_GET_ENDPOINT + "apikey=" + API_KEY + "&url=" + url;

    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(requestUrl);

    try {
      CloseableHttpResponse response = client.execute(httpGet);
      BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
      }

      JSONObject jsonResult = new JSONObject(result.toString());
      return jsonResult.getJSONArray("ParsedResults").getJSONObject(0).getString("ParsedText");
    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return null;
  }

  public String selectRandomUA() {
    List<String> userAgents = new ArrayList<String>();
    Random rand = new Random();
    try (BufferedReader br = new BufferedReader(new FileReader(USERAGENT_FILEPATH))) {
      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null) {
        userAgents.add(sCurrentLine);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return userAgents.get(rand.nextInt(userAgents.size()));
  }

  public List<String> extractData(String data) {
    List<String> domainList = new ArrayList<>();

    if (data != null) {
      String domainsData = data.split(DATA_INTRO_DELIMITER)[1];
      String[] domainArray = domainsData.split(DATA__DELIMITER);
      for (String domain : domainArray) {
        String domainFormatted = checkFormat(domain);
        if (domainFormatted != null) {
          domainList.add(domainFormatted);
        }
      }
    }

    return domainList;
  }

  public String checkFormat(final String domain) {
    String formattedDomain = null;

    if (domain != null && !domain.isEmpty()) {

      String domainThreeLastChar = domain.substring(domain.length() - 3, domain.length());

      switch (domainThreeLastChar) {
        case ".fr":
          formattedDomain = domain;
          break;
        case " fr":
          formattedDomain = domain.replace(" fr", ".fr");
          break;
        default:
          // case missing '.'
          formattedDomain = domain.substring(0, (domain.length() - 2)).concat(".fr");
      }
    }

    return formattedDomain;
  }

  public List<Contact> whois(final String domain, final boolean retrieve) {
    List<Contact> contacts = new ArrayList<>();
    try {
      Map<String, String> listHeaders = new HashMap<>();
      listHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      listHeaders.put("Accept-Encoding", "gzip, deflate, br");
      listHeaders.put("Accept-Language", "en-US,en;q=0.9,fr-FR;q=0.8,fr;q=0.7,la;q=0.6");
      listHeaders.put("Connection", "keep-alive");
      listHeaders.put("Pragma", "no-cache");
      listHeaders.put("Host", "ksah.in");
      listHeaders.put("User-Agent", selectRandomUA());

      HttpResponse<String> response = Unirest.get("https://urlscan.io/domain/" + domain).headers(listHeaders).asString();
      Document doc = Jsoup.parse(response.getBody());
      String test = doc.outerHtml();
      String whoisDataFullBloc = test.split("<pre>")[1].split("</pre>")[0];
      String[] splittedBlocs = whoisDataFullBloc.split("\\n\\n");

      for (String bloc : splittedBlocs) {
        Contact contact = extractWhoisBlocData(bloc, domain);
        if (contact != null) {
          contacts.add(contact);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      if (!retrieve) {
        this.domainToRetrieve.add(domain);
        e.printStackTrace();
      } else {
        throw e;
      }
    } catch (IllegalArgumentException | UnirestException e) {
      e.printStackTrace();
    }

    return contacts;
  }

  public Contact extractWhoisBlocData(final String bloc, final String domain) {
    Contact contact = new Contact();

    String[] rows = bloc.split("\\n");
    Map<String, String> rowsKeyValue = new HashMap<>();
    for (String row : rows) {
      try {
        if (row != null && !row.isEmpty()) {
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
      contact.setDomainName(domain);
      contact.setEmail(email);
      contact.setType(type);
      contact.setPhone(rowsKeyValue.get("phone"));
    }

    return (contact.getEmail() == null) ? null : contact;
  }

  public void saveData(List<Contact> listContacts) {
    for (Contact contact : listContacts) {
      try {
        contactDAO.insert(contact);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
