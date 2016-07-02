package org.onehippo;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HSTScaffold {

    final static Logger log = Logger.getLogger(HSTScaffold.class);

    final public static String SCAFFOLD_DIR_NAME = ".scaffold";

    public static final String PROJECT_HST_NODE_NAME = "myhippoproject";

    public static final String PROJECT_DIR = "projectDir";
    public static final String DEFAULT_PROJECT_DIR = ".";
    public static final String PROJECT_PACKAGE_NAME = "projectPackageName";

    public static final String TEMPLATE_PATH = "templatePath";
    public static final String DEFAULT_TEMPLATE_PATH = "bootstrap/webfiles/src/main/resources";

    public static final String JAVA_COMPONENT_PATH = "componentPath";
    public static final String DEFAULT_COMPONENT_PATH = "site/src/main/java";

    public static final Pattern COMMENT = Pattern.compile("^(\\s*)#.*");
    public static final Pattern URL = Pattern.compile("^(\\s*)(/[^\\s]*/?)*");
    public static final Pattern CONTENT = Pattern.compile("^(\\s*)(/[^\\s]*/?)*");
    public static final Pattern PAGE = Pattern.compile("([\\w\\(\\),\\s]+)");

    private static HSTScaffold scaffold;
    private ScaffoldBuilder builder;

    private List<Route> routes = new ArrayList<Route>();

    public static Properties properties;
    {
        properties = new Properties();
        try {
            final InputStream stream = this.getClass().getResourceAsStream("/scaffold.properties");
            log.debug("stream "+stream);
            properties.load(stream);
        } catch (IOException e) {
            log.error("Error loading properties");
        }
    }

    HSTScaffold() {
        // todo load config from parameter
        read(new InputStreamReader(this.getClass().getResourceAsStream("/scaffold.hst")));
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void read(Reader configReader) {
        try {
            StringBuilder configBuilder = new StringBuilder();

            BufferedReader reader = new BufferedReader(configReader);
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

    private boolean isComment(String line) {
        Matcher matcher = COMMENT.matcher(line);
        if (matcher.matches()) {
            log.debug("skip line "+line);
            return true;
        }
        return false;
    }

    public void read(String config) {
        // *name, :id
        // /text/*path       /contact/path:String    text(header,main(banner, text),footer)
        Scanner scanner = new Scanner(config);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (isComment(line)) {
                continue;
            }

            Matcher matcher = URL.matcher(line);
            if (!matcher.find() || StringUtils.isEmpty(matcher.group(2))) {
                log.warn("Invalid route, url: "+line);
                continue;
            }
            String url = matcher.group(2);
            line = line.substring(matcher.group(1).length()+matcher.group(2).length());

            matcher = CONTENT.matcher(line);
            if (!matcher.find() || StringUtils.isEmpty(matcher.group(2))) {
                log.warn("Invalid route, content: "+line);
                continue;
            }
            String content = matcher.group(2);
            line = line.substring(matcher.group(1).length()+matcher.group(2).length());

            matcher = PAGE.matcher(line);
            if (!matcher.find()) {
                log.warn("Invalid route, page: "+line);
                continue;
            }

            String page = matcher.group(1);
            routes.add(new Route(url, content, page));
        }

    }

    public void setBuilder(ScaffoldBuilder builder) {
        this.builder = builder;
    }

    public void dryRun() {
        if (this.builder != null) {
            this.builder.dryRun();
        }
    }

    public void build() {
        if (this.builder != null) {
            this.builder.build();
        }
    }

    public void rollback() {
        if (this.builder != null) {
            this.builder.rollback();
        }
    }

    public static HSTScaffold instance() {
        if (scaffold == null) {
            scaffold = new HSTScaffold();
        }

        return scaffold;
    }

}
