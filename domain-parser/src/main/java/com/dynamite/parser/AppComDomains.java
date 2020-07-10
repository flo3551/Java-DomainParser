
package com.dynamite.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.dynamite.bean.Contact;
import com.dynamite.config.GetPropertyValues;
import com.dynamite.dao.ContactDAO;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class AppComDomains {

  private static final GetPropertyValues props = new GetPropertyValues();

  public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  private static final String URL_PREFIX = "https://whoisds.com//whois-database/newly-registered-domains/";

  private static final String URL_SUFFIX = "/nrd";

  private static final String PATH_ZIP_FILE = "/tmp/zipFile.zip";

  private static final String PATH_TXT_FILE = "/tmp/txtFile.txt";

  private static final String USERAGENT_FILEPATH = "./src/main/ressources/ua.txt";

  public List<String> domainToRetrieve = new ArrayList<>();

  public List<Contact> listContact = new ArrayList<>();

  public int nbRetrieveAttempts = 0;

  private ContactDAO contactDAO = new ContactDAO();

  public int indexUrl;

  public static void main(String[] args) throws IOException {
    try {
      AppComDomains app = new AppComDomains();
      app.launch();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      deleteFile(PATH_ZIP_FILE);
      deleteFile(PATH_TXT_FILE);
    }
  }

  public void launch() throws IOException {
    FileInputStream domainListFile = this.getDomainListDownloadedTxt();
    List<String> domainList = filterWantedDomains(domainListFile);

    listContact = new ArrayList<>();
    int index = 0;

    for (String domain : domainList) {
      System.out.println("___________________________________________");
      System.out.println("domain " + index + "of " + domainList.size());
      System.out.println("timeoutDomains : " + domainToRetrieve.size());
      sleep(150);
      listContact.addAll(whoisFromUrlScan(domain, false));
      if (listContact.size() > 1000) {
        saveData(listContact);
        listContact.clear();
      }
      System.out.println("unfilter contact number :" + listContact.size());
      System.out.println("___________________________________________");
      index++;
    }

    retrieveFailedDomains();

    saveData(listContact);
  }

  public void retrieveFailedDomains() {
    int initialNumberToRetrieve = domainToRetrieve.size();
    int nbRetrieveAttempsMax;

    try {
      nbRetrieveAttempsMax = Integer.parseInt(props.getPropValue("numberAttempts"));
    } catch (NullPointerException | IOException e) {
      nbRetrieveAttempsMax = 0;
    }

    if (domainToRetrieve.size() > 0) {
      nbRetrieveAttempts = 0;
      while (nbRetrieveAttempts < nbRetrieveAttempsMax && domainToRetrieve.size() > 0) {
        List<String> hasSucceededDomains = new ArrayList<String>();

        for (String domain : domainToRetrieve) {
          System.out.println("___________________________________________");
          System.out.println("attempt" + nbRetrieveAttempts);
          System.out.println("domain " + domainToRetrieve.indexOf(domain) + "of " + domainToRetrieve.size());
          System.out.println("timeoutDomains : " + domainToRetrieve.size());

          boolean hasSucceeded = false;
          try {
            sleep(3000);
            this.listContact.addAll(whoisFromUrlScan(domain, true));
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

  public FileInputStream getDomainListDownloadedTxt() throws IOException {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate date = LocalDate.now().minusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String yyyyMMdd = sdf.format(Date.from(date.atStartOfDay(defaultZoneId).toInstant()));

    String urlFilePart = Base64.getEncoder().encodeToString((yyyyMMdd + ".zip").getBytes());
    Map<String, String> listHeaders = new HashMap<>();
    listHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    listHeaders.put("Accept-Encoding", "gzip, deflate, br");
    listHeaders.put("Accept-Language", "en-US,en;q=0.9,fr-FR;q=0.8,fr;q=0.7,la;q=0.6");
    listHeaders.put("Connection", "keep-alive");
    listHeaders.put("Pragma", "no-cache");
    listHeaders.put("User-Agent", selectRandomUA());

    HttpResponse<File> response = Unirest.post(URL_PREFIX + urlFilePart + URL_SUFFIX).headers(listHeaders).asFile(PATH_ZIP_FILE);
    byte[] buffer = new byte[2048];

    // open the zip file stream
    InputStream theFile = new FileInputStream(response.getBody());
    ZipInputStream stream = new ZipInputStream(theFile);
    ZipEntry entry;
    try {

      // now iterate through each item in the stream. The get next
      // entry call will return a ZipEntry for each file in the
      // stream
      while ((entry = stream.getNextEntry()) != null) {
        String s = String.format("Entry: %s len %d added %TD", entry.getName(), entry.getSize(), new Date(entry.getTime()));
        System.out.println(s);

        // Once we get the entry from the stream, the stream is
        // positioned read to read the raw data, and we keep
        // reading until read returns 0 or less.
        FileOutputStream output = null;
        try {
          output = new FileOutputStream(PATH_TXT_FILE);
          int len = 0;
          while ((len = stream.read(buffer)) > 0) {
            output.write(buffer, 0, len);
          }
        } finally {
          // we must always close the output file
          if (output != null)
            output.close();
        }
      }
    } finally {
      // we must always close the zip file.
      stream.close();
    }

    // READING TXT FILE
    FileInputStream fis = new FileInputStream(PATH_TXT_FILE);

    // TODO: return fileInputStream & delete local zipfile + txtFile
    return fis;
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

  public List<Contact> whoisFromUrlScan(final String domain, final boolean retrieve) {
    List<Contact> contacts = new ArrayList<>();
    try {
      Map<String, String> listHeaders = new HashMap<>();
      listHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      listHeaders.put("Accept-Encoding", "gzip, deflate, br");
      listHeaders.put("Accept-Language", "en-US,en;q=0.9,fr-FR;q=0.8,fr;q=0.7,la;q=0.6");
      listHeaders.put("Connection", "keep-alive");
      listHeaders.put("Pragma", "no-cache");
      listHeaders.put("User-Agent", selectRandomUA());

      HttpResponse<String> response = Unirest.get("https://urlscan.io/domain/" + domain).headers(listHeaders).asString();
      Document doc = Jsoup.parse(response.getBody());
      if (doc.getElementsByTag("pre").size() > 0) {
        String outerHtml = doc.outerHtml();
        String whoisDataFullBloc = outerHtml.split("<pre>")[1].split("</pre>")[0];

        Contact contact = extractDataFromWhoisSection(whoisDataFullBloc, domain);
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

  public Contact extractDataFromWhoisSection(final String bloc, final String domain) {
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

    if (email != null && validate(email)) {
      contact.setAddress(rowsKeyValue.get("Registrant Street"));
      contact.setContact(rowsKeyValue.get("Registrant Name"));
      contact.setCountry(rowsKeyValue.get("Registrant Country"));
      contact.setDomainName(domain);
      contact.setEmail(rowsKeyValue.get("Registrant Email"));
      contact.setType("REGISTRANT");
      contact.setPhone(rowsKeyValue.get("Registrant Phone"));
    }

    return (contact.getEmail() == null) ? null : contact;
  }

  public List<String> filterWantedDomains(FileInputStream fis) throws IOException {
    List<String> domains = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
    while (reader.ready()) {
      // Every line is a domain
      String line = reader.readLine();

      if (".com".equalsIgnoreCase(line.substring(line.length() - 4, line.length()))) {
        domains.add(line.trim());
      }
    }

    return domains;
  }

  public void saveData(List<Contact> listContacts) {
    List<Contact> duplicatesEmails = filterContacts(listContact);
    listContact.removeAll(duplicatesEmails);

    System.out.print("Adding " + listContacts.size() + " contacts");
    for (Contact contact : listContacts) {
      try {
        contactDAO.insert(contact);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public static void deleteFile(String path) {
    File file = new File(path);
    if (file.delete()) {
      System.out.println(file.getName() + " deleted");
    } else {
      System.out.println(file.getName() + " deletion FAILED");
    }
  }

  public static boolean validate(String emailStr) {
    Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
    return matcher.find();
  }

  public void sleep(long millis) {
    System.out.println("sleep " + millis);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
