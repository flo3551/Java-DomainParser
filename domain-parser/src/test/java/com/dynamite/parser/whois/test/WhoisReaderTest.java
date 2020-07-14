
package com.dynamite.parser.whois.test;

import org.junit.Test;

import com.dynamite.parser.whois.WhoisReader;

public class WhoisReaderTest {

  private WhoisReader reader = new WhoisReader();

  @Test
  public void test1_whoisDomainsList() {
    reader.launch();
  }
}
