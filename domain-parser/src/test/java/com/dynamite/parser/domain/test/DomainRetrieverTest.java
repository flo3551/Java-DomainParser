
package com.dynamite.parser.domain.test;

import org.junit.Test;

import com.dynamite.parser.domain.DomainRetriever;

public class DomainRetrieverTest {

  private DomainRetriever domainRetriever = new DomainRetriever();

  @Test
  public void test1_getAllDomains() {
    domainRetriever.getAllDomains();
  }
}
