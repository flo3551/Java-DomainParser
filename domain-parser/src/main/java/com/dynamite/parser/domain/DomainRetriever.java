
package com.dynamite.parser.domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import com.dynamite.bean.Contact;
import com.dynamite.bean.Domain;
import com.dynamite.dao.DomainDAO;

import helper.RequestHelper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class DomainRetriever {

  private static final String PATH_ZIP_FILE = "/tmp/zipFile.zip";

  private static final String PATH_TXT_FILE = "/tmp/txtFile.txt";

  private static final String URL_FIRST_PART = "https://www.afnic.fr/data/divers/public/publication-quotidienne/";

  private static final String URL_SECOND_PART = "_CREA_fr.gif";

  private static final String OCR_API_KEY = "4f4f70b8f388957";

  private static final String OCR_API_GET_ENDPOINT = "https://api.ocr.space/parse/imageurl?";

  private static final String DATA_INTRO_DELIMITER = "\\*BOF";

  private static final String DATA__DELIMITER = "\\r\\n";

  private static final String URL_PREFIX = "https://whoisds.com/whois-database/newly-registered-domains/";

  private static final String URL_SUFFIX = "/nrd";

  private DomainDAO domainDAO = new DomainDAO();

  private RequestHelper requestHelper = new RequestHelper();

  public static void main(String[] args) throws IOException {
    DomainRetriever domainRetriever = new DomainRetriever();
    domainRetriever.getAllDomains();
  }

  public void getAllDomains() {
    List<Domain> domainsList = new ArrayList();

    domainsList.addAll(getDomainsFr());
    domainsList.addAll(getDomainsDotCom());

    saveData(domainsList);
  }

  public List<Domain> getDomainsFr() {
    String url = this.getDomainListImageUrl();
    String parsedText = getOCRFromImage(url);
    List<Domain> domainList = extractDataFromAFNICDomainList(parsedText);

    return domainList;
  }

  public List<Domain> getDomainsDotCom() {
    List<Domain> domainList = new ArrayList<Domain>();
    try {
      FileInputStream domainListFile = this.getDomainListDownloadedTxt();
      domainList = filterWantedDomains(domainListFile);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      deleteFile(PATH_ZIP_FILE);
      deleteFile(PATH_TXT_FILE);
    }

    return domainList;
  }

  public String getDomainListImageUrl() {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate date = LocalDate.now().minusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String yyyyMMdd = sdf.format(Date.from(date.atStartOfDay(defaultZoneId).toInstant()));

    return URL_FIRST_PART + yyyyMMdd + URL_SECOND_PART;
  }

  public String getOCRFromImage(final String url) {
    String requestUrl = OCR_API_GET_ENDPOINT + "apikey=" + OCR_API_KEY + "&url=" + url;

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

  public List<Domain> extractDataFromAFNICDomainList(String data) {
    List<Domain> domainList = new ArrayList<>();

    if (data != null) {
      String domainsData = data.split(DATA_INTRO_DELIMITER)[1];
      String[] domainArray = domainsData.split(DATA__DELIMITER);
      for (String domain : domainArray) {
        String domainFormatted = checkFormat(domain);
        if (domainFormatted != null) {
          domainList.add(new Domain(domainFormatted, "AFNIC"));
        }
      }
    }

    return domainList;
  }

  public String checkFormat(final String domain) {
    String formattedDomain = null;

    if (domain != null && !domain.isEmpty() && domain.length() > 4 && !domain.contains("#")) {

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

  public Contact extractDataFromWhoisSection(final String bloc, final String domain) {
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
    listHeaders.put("User-Agent", requestHelper.selectRandomUA());

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

  public List<Domain> filterWantedDomains(FileInputStream fis) throws IOException {
    List<Domain> domains = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
    while (reader.ready()) {
      // Every line is a domain
      String line = reader.readLine();

      if (".com".equalsIgnoreCase(line.substring(line.length() - 4, line.length()))) {
        domains.add(new Domain(line.trim(), "WHOISDS"));
      }
    }

    return domains;
  }

  public void saveData(List<Domain> domainList) {
    try {
      domainDAO.insertAll(domainList);
    } catch (SQLException e) {
      e.printStackTrace();
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

}