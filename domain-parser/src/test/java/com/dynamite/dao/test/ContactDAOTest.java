
package com.dynamite.dao.test;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import com.dynamite.bean.Contact;
import com.dynamite.dao.ContactDAO;

public class ContactDAOTest {

  private ContactDAO contactDAO;

  @Before
  public void setup() {
    contactDAO = new ContactDAO();
  }

  @Test
  public void testInsertContact() {
    Contact contact = new Contact("email", "domain", "contact", "adresse", "phone", "type", "country", false);
    Integer result = null;
    try {
      result = contactDAO.insert(contact);
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertTrue(result > 0);
  }
}
