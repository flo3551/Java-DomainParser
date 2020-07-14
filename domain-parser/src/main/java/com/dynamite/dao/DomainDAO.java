
package com.dynamite.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.dynamite.bean.Domain;

public class DomainDAO {

  private static final String SQL_INSERT = "INSERT INTO domain(domain, date, source) VALUES (?,?,?)";

  private static final String SQL_DELETE = "DELETE from domain where domain = ?";

  private static final String SQL_COUNT = "SELECT COUNT(*) as numberDomains from domain";

  private static final String SQL_SELECT_1000 = "SELECT * from domain ORDER BY date LIMIT 1000";

  public int insert(final Domain domain) throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, domain.getDomainName());
      ps.setString(2, domain.getDate());
      ps.setString(3, domain.getSource());

      result = ps.executeUpdate();
      ps.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public int insertAll(final List<Domain> domainsList) throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      conn.setAutoCommit(false);

      for (Domain domain : domainsList) {
        PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, domain.getDomainName());
        ps.setString(2, domain.getDate());
        ps.setString(3, domain.getSource());

        ps.executeUpdate();
        ps.close();
      }
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public int deleteByDomain(final Domain domain) throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      PreparedStatement ps = conn.prepareStatement(SQL_DELETE, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, domain.getDomainName());

      result = ps.executeUpdate();
      ps.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public int deleteDomains(final List<Domain> domainsList) throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      conn.setAutoCommit(false);
      
      for (Domain domain : domainsList) {
        PreparedStatement ps = conn.prepareStatement(SQL_DELETE, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, domain.getDomainName());        
        result = ps.executeUpdate();
        ps.close();
      }
      
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }
  
  public List<Domain> selectDomainsList() throws SQLException {
    List<Domain> domainsList = new ArrayList<>();

    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      PreparedStatement ps = conn.prepareStatement(SQL_SELECT_1000, Statement.RETURN_GENERATED_KEYS);

      ResultSet result = ps.executeQuery();

      while (result.next()) {
        Domain domain = new Domain(result.getString("domain"), result.getString("date"), result.getString("source"));
        domainsList.add(domain);
      }

      ps.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return domainsList;
  }

  public int countDomains() throws SQLException {
    int result = 0;
    try {
      String url = "jdbc:mysql://127.0.0.1:3306/dynamite_parser_database";
      Connection conn = DriverManager.getConnection(url, "root", "dealmeida");
      PreparedStatement ps = conn.prepareStatement(SQL_COUNT, Statement.RETURN_GENERATED_KEYS);

      ResultSet resultSet = ps.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getInt("numberDomains");
      }

      ps.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }
}
