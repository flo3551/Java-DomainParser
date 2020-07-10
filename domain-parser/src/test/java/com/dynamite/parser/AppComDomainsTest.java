
package com.dynamite.parser;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class AppComDomainsTest {

  private AppComDomains app;

  private String url;

  @Before
  @Test
  public void setup() {
    app = new AppComDomains();
    assertNotNull(app);
  }

  @Test
  public void Test_realTest() {
    try {
      app.launch();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      AppComDomains.deleteFile("/tmp/zipFile.zip");
      AppComDomains.deleteFile("/tmp/txtFile.txt");
    }
  }
}
