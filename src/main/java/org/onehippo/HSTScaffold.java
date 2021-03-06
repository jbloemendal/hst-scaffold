package org.onehippo;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.build.ScaffoldBuilder;
import org.onehippo.fold.FileFolder;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HSTScaffold {

    final static Logger log = Logger.getLogger(HSTScaffold.class);

    final public static String SCAFFOLD_DIR_NAME = ".scaffold";

    public static final String PROJECT_NAME = "projectName";
    public static final String WEBFILE_BASE_PATH = "webfileBasePath";

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
    public static final Pattern PAGE = Pattern.compile("([\\w\\(\\),\\s&\\*]+)");

    private static HSTScaffold scaffold;
    private ScaffoldBuilder builder;
    private FileFolder folder;

    private List<Route> routes = new ArrayList<Route>();

    public static Properties properties;

    private File projectDir;

    HSTScaffold(String projectDirPath) throws IOException {
        this.projectDir = new File(projectDirPath);
        File scaffoldDir = createHiddenScaffold(projectDirPath);

        copyDefaultFiles(scaffoldDir);
        loadProperties(scaffoldDir);

        File scaffoldConf = new File(projectDirPath, "scaffold.hst");
        if (scaffoldConf.exists()) {
            read(new InputStreamReader(new FileInputStream(scaffoldConf)));
            return;
        }
        throw new IOException(String.format("Scaffold configuration scaffold.hst is missing %s.", scaffoldConf.getAbsolutePath()));
    }

    private void loadProperties(File scaffoldDir) {
        properties = new Properties();
        try {
            File propertiesFile = new File(scaffoldDir, "conf.properties");

            if (!propertiesFile.exists()) {
                FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("/scaffold.properties"), propertiesFile);
            }

            properties.load(new BufferedInputStream(new FileInputStream(propertiesFile)));
            properties.put(PROJECT_DIR, scaffoldDir.getParent());

            // todo
            File projectPom = new File(scaffoldDir.getParentFile(), "pom.xml");

            // can we determine project name and project package name from the existing sources
            // and what about the hst site conf name / multi site setups?

            // properties.store();
        } catch (IOException e) {
            log.error("Error loading properties");
        }
    }

    private File createHiddenScaffold(String projectDirPath) throws IOException {
        File projectDir = new File(projectDirPath);
        if (projectDir.exists()) {
            File scaffoldDir = new File(projectDir, ".scaffold");
            if (!scaffoldDir.exists()) {
                scaffoldDir.mkdirs();
            }
            return scaffoldDir;
        } else {
            throw new IOException(String.format("Project directory doesn't exist %s.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }
    }

    private void copyDefaultFiles(File scaffoldDir) throws IOException {
        File propertiesFile = new File(scaffoldDir, "conf.properties");
        if (!propertiesFile.exists()) {
            FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("/scaffold.properties"), propertiesFile);
        }

        File componentJava = new File(scaffoldDir, "Component.java.mustache");
        if (!componentJava.exists()) {
            FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("/Component.java.mustache"), componentJava);
        }

        File templateFtl = new File(scaffoldDir, "template.ftl.mustache");
        if (!templateFtl.exists()) {
            FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("/template.ftl.mustache"), templateFtl);
        }
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
            if (matcher.find() && StringUtils.isNotEmpty(matcher.group(2))) {
                String url = matcher.group(2);
                line = line.substring(matcher.group(1).length()+matcher.group(2).length());

                matcher = CONTENT.matcher(line);
                if (matcher.find() && StringUtils.isNotEmpty(matcher.group(2))) {
                    String content = matcher.group(2);
                    line = line.substring(matcher.group(1).length()+matcher.group(2).length());

                    matcher = PAGE.matcher(line);
                    if (matcher.find()) {
                        String page = matcher.group(1);
                        routes.add(new Route(url, content, page));
                    } else {
                        log.warn("Invalid route, page: "+line);
                        continue;
                    }
                } else {
                    log.warn("Invalid route, content: "+line);
                    continue;
                }
            } else {
                log.warn("Invalid route, url: "+line);
                continue;
            }
        }

    }

    public void setBuilder(ScaffoldBuilder builder) {
        this.builder = builder;
    }

    public void setScafFolder(FileFolder folder) {
        this.folder = folder;
    }

    public void build(boolean dryRun) {
        if (this.builder != null) {
            try {
                this.builder.backup(dryRun);
                this.builder.build(dryRun);
            } catch (Exception e) {
                log.error("Error building scaffold, rolling back.", e);
            }
        }
    }

    public void rollback(boolean dryRun) {
        if (this.builder != null) {
            try {
                this.builder.rollback(dryRun);
            } catch (Exception e) {
                log.error("Error rolling back to previous project state.");
            }
        }
    }

    public Map<String, Route> scaffold(File destination, boolean dryRun) {
        if (this.folder != null) {
            try {
                return this.folder.fold(destination, dryRun);
            } catch (Exception e) {
                log.error("Error folding project configuration.", e);
            }
        }
        return null;
    }

    public static HSTScaffold instance(String path) throws IOException {
        if (scaffold == null) {
            scaffold = new HSTScaffold(path);
        }

        return scaffold;
    }

    public File getProjectDir() {
        return projectDir;
    }
}
