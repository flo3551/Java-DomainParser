
package helper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequestHelper {

  private static final String USERAGENT_FILEPATH = "./src/main/ressources/ua.txt";

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
}
