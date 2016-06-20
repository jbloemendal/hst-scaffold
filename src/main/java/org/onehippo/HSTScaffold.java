package org.onehippo;


import org.apache.log4j.Logger;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

public class HSTScaffold {

    final static Logger log = Logger.getLogger(HSTScaffold.class);

    final public static String SCAFFOLD_DIR_NAME = ".scaffold";

    public static final String PROJECT_DIR = "PROJECT_DIR";

    public static final String TEMPLATE_PATH = "TEMPLATE_PATH";

    public static final String JAVA_COMPONENT_PATH = "JAVA_COMPONENT_PATH";

    private static HSTScaffold scaffold;

    private List<Route> routes;

    public List<Route> getRoutes() {
        return routes;
    }

    public void read(File config) {
        try {
            StringBuilder configBuilder = new StringBuilder();

            BufferedReader reader = new BufferedReader(new FileReader(config));
            char [] buffer = new char[1024];
            while (reader.read(buffer) > 0) {
                configBuilder.append(buffer);
            }

            read(configBuilder.toString());
        } catch (FileNotFoundException e) {
            log.error("Error reading configuration file, file not found.", e);
        } catch (IOException e) {
            log.error("Error reading configuration file.", e);
        }
    }

    public void read(String config) {

        Scanner scanner = new Scanner(config);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            Scanner lineScanner = new Scanner(line);
            if (lineScanner.hasNext(Pattern.compile("^\\s*#"))) {
                break;
            }

            // *name, :id
            // /text/*path       /contact/path:String    text(header,main(banner, text),footer)
            Pattern urlPattern = Pattern.compile("\\s*(/[^/\\s]]/?)+");
            if (!lineScanner.hasNext(urlPattern)) {
                log.warn("Invalid route: "+line);
                continue;
            }
            String urlMatcher = lineScanner.next(urlPattern);

            Pattern contentPattern = Pattern.compile("\\s*(/[^/\\s]]/?)+");
            if (!lineScanner.hasNext(contentPattern)) {
                log.warn("Invalid route: "+line);
                continue;
            }
            String contentPath = lineScanner.next(contentPattern);

            Pattern pagePattern = Pattern.compile("[\\w\\(\\),\\s]+");
            if (!lineScanner.hasNext(pagePattern)) {
                log.warn("Invalid route: "+line);
                continue;
            }
            String pageConstruct = lineScanner.next(pagePattern);

            routes.add(new Route(urlMatcher, contentPath, pageConstruct));
        }

    }

    public static Properties getConfig() {
        Properties properties = new Properties();
        properties.put(PROJECT_DIR, "");
        properties.put(TEMPLATE_PATH, "");
        properties.put(JAVA_COMPONENT_PATH, "");
        return properties;
    }

    public static HSTScaffold instance() {
        if (scaffold == null) {
            scaffold = new HSTScaffold();
        }
        // todo load properties
        return scaffold;
    }

}
