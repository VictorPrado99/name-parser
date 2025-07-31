import managers.NameSearchManager;
import managers.impl.NameSearchManagerImpl;

import java.net.URI;
import java.net.URL;

public class NameParser {

    public static void main(String[] args) throws Exception {
        URL dictionaryUrl = NameParser.class.getClassLoader().getResource("dictionary.txt");
        URL bigUrl = NameParser.class.getClassLoader().getResource("big.txt");

        if(dictionaryUrl == null || bigUrl == null ) throw new Exception("Missing set file");

        URI dictionaryUri = dictionaryUrl.toURI();
        URI fileToParseUri = bigUrl.toURI();

        NameSearchManager nameSearchManager = new NameSearchManagerImpl(dictionaryUri, fileToParseUri);

        nameSearchManager.execute();

    }

}
