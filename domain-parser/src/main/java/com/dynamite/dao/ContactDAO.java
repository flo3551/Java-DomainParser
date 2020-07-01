
package com.dynamite.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.dynamite.bean.Contact;

public class ContactDAO {

  private static final String SQL_INSERT = "INSERT INTO contact(email, domainName, typeContact, phone, address, contact, country) VALUES (?,?,?,?,?,?,?)";

  // @Autowired
  // private DataSource dataSource;

  public int insert(final Contact contact) throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, contact.getEmail());
      ps.setString(2, contact.getDomainName());
      ps.setString(3, contact.getType());
      ps.setString(4, contact.getPhone());
      ps.setString(5, contact.getAddress());
      ps.setString(6, contact.getContact());
      ps.setString(7, contact.getCountry());

      result = ps.executeUpdate();
      ps.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }
}
