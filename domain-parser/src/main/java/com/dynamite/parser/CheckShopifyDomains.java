
package com.dynamite.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.dynamite.bean.Contact;
import com.dynamite.config.GetPropertyValues;
import com.dynamite.dao.ContactDAO;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class CheckShopifyDomains {
  
  private ContactDAO contactDAO = new ContactDAO();
  public List<Contact> listContact = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    CheckShopifyDomains app = new CheckShopifyDomains();
    app.launch();
  }

  public void launch() throws IOException {
//    List<String> domainList = contactDAO.getByMissingShopify();

    listContact = new ArrayList<>();
    int index = 0;

//    for (String domain : domainList) {
      // Find a way to check browser Window object.
      // Headless browser
//
//    }

//    contactDAO.updateAll();
  }

}
