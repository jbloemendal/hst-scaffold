package org.onehippo;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.*;

public class RepositoryBuilder implements ScaffoldBuilder {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private Node hstRoot;
    private Node projectHstConfRoot;
    private File projectDir;
    private File scaffoldDir;

    public RepositoryBuilder(Node hstRoot) throws RepositoryException, IOException {
        this.hstRoot = hstRoot;

        String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        projectHstConfRoot = hstRoot.getNode("hst:configurations").getNode(projectHstNodeName);

        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
        if (!projectDir.exists()) {
            throw new IOException(String.format("Project directory doesn't exist %s.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }

        scaffoldDir = new File(projectDir, ".scaffold");
        if (!scaffoldDir.exists()) {
            scaffoldDir.mkdirs();
        }

    }

    private void backup(boolean dryRun) throws IOException, RepositoryException {
        File backup = new File(scaffoldDir, "history/"+System.currentTimeMillis());
        log.info(String.format("Creating backup directory %s", backup.getPath()));
        if (!dryRun) {
            backup.mkdirs();
        }

        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

        File hstConfFile = new File(backup, projectName+"_hst.xml");
        log.info(String.format("Export hst \"%s\" config %s", projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new FileOutputStream(hstConfFile);
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }

        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);

        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);
        log.info(String.format("Backup java components %s and templates %s", componentDirectory.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(componentDirectory, new File(backup, "java"));
            FileUtils.copyDirectory(templateDirectory, new File(backup, "ftl"));
        }
    }

    public void build(boolean dryRun) throws IOException, RepositoryException {
        backup(dryRun);

        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        for (Route route : scaffold.getRoutes()) {
            try {
                buildComponent(route.getPage(), dryRun);
                buildSitemapItem(route, dryRun);
            } catch (IOException e) {
                log.error("Error building route.", e);
            } catch (RepositoryException e) {
                log.error("Error building route.", e);
            }
        }

    }

    private void buildComponentJavaFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(this.getClass().getResource("/Component.java.mustache").getFile());

        Writer writer= new PrintWriter(System.out);;
        File javaClassFile = new File(component.getPathJavaClass());
        javaClassFile.getParentFile().mkdirs();

        log.info(String.format("Build component %s java class %s", component.getName(), javaClassFile.getPath()));

        if (!dryRun) {
            if (javaClassFile.exists()) {
                log.info(String.format("Java component file %s alreday exists", javaClassFile.getPath()));
            } else {
                writer = new FileWriter(javaClassFile);
            }
        }

        try {
            mustache.execute(writer, new HashMap<String, String>() {
                {
                    put("projectPackage", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_PACKAGE_NAME));
                    put("name", StringUtils.capitalize(component.getName()));
                }
            }).flush();
        } finally {
            writer.close();
        }
    }

    private void buildTemplateFtlFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(this.getClass().getResource("/template.ftl.mustache").getFile());

        Writer writer = new PrintWriter(System.out);

        File templateFile = new File(component.getTemplateFilePath());

        templateFile.getParentFile().mkdirs();

        log.info(String.format("Build %s component template %s", component.getName(), templateFile.getPath()));

        if (!dryRun) {
            if (templateFile.exists()) {
                log.info(String.format("Template file %s already exists.", templateFile.getPath()));
            } else {
                writer = new FileWriter(templateFile);
            }
        }

        try {
            mustache.execute(writer, new HashMap<String, Object>() {
                {
                    put("childs", component.getComponents());
                    put("name", StringUtils.capitalize(component.getName().toLowerCase()));
                }
            }).flush();
        } finally {
            writer.close();
        }
    }

    private void buildComponentHstConf(Route.Component component, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:components")) {
            throw new RepositoryException("HST configuration child hst:components misses.");
        }

        Node components = projectHstConfRoot.getNode("hst:components");
        String nodeName = component.getName();
        int index = 1;
        while (components.hasNode(nodeName)) { // todo ???
            nodeName = component.getName()+"_"+index;
            index++;
        }

        log.info(String.format("New component node %s", components.getPath()+"/"+nodeName+"."));
        if (!dryRun) {
            Node newComponent = components.addNode(nodeName, "hst:component");
            newComponent.setProperty("hst:componentclassname", component.getJavaClass());
            newComponent.setProperty("hst:template", component.getTemplateName());
        }
    }

    private void buildComponent(Route.Component component, boolean dryRun) throws IOException, RepositoryException {
        log.debug(String.format("Build %s java file", component.getName()));
        buildComponentJavaFile(component, dryRun);

        log.debug(String.format("Build %s template", component.getName()));
        buildTemplateFtlFile(component, dryRun);

        log.debug(String.format("Build %s hst conf", component.getName()));
        buildComponentHstConf(component, dryRun);

        // jcr hst configuration
        for (Route.Component child : component.getComponents()) {
            buildComponent(child, dryRun);
        }
    }

    private void buildSitemapItem(Route route, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:sitemap")) {
            throw new RepositoryException("HST configuration child hst:sitemap misses.");
        }

        Scanner scanner = new Scanner(route.getUrl());

        Node sitemapItem = projectHstConfRoot.getNode("hst:sitemap");;
        while (scanner.hasNext()) {
            String path = scanner.next();
            if (path.startsWith(":")) {
                sitemapItem.addNode("_default_", "hst:sitemapitem");
                sitemapItem.setProperty("hst:componentconfigurationid", "");
            } else if (path.startsWith("*")) {
                sitemapItem.addNode("_any_", "hst:sitemapitem");
                sitemapItem.setProperty("hst:componentconfigurationid", "");
            } else {
                sitemapItem.addNode(path, "hst:sitemapitem");
            }
        }

        String contentPath = route.getContentPath();
        int index = 1;
        for (Route.Parameter param : route.getParameters()) {
            contentPath.replace(param.name+":"+param.type, "${"+index+"}");
            index++;
        }

        sitemapItem.setProperty("hst:componentconfigurationid", "hst:pages/"+route.getPage().getName());
        sitemapItem.setProperty("hst:relativecontentpath", contentPath.substring(1));

    }


    public void rollback(boolean dryRun) throws IOException, RepositoryException {
        log.info("Move current project conf into trash.");
        File trash = new File(scaffoldDir, "trash/"+System.currentTimeMillis());
        if (!trash.exists()) {
            trash.mkdirs();
        }

        // move current projects java and templates into .scaffold/.trash/date
        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);
        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);

        log.info(String.format("Move java components %s and templates %s into %s", componentDirectory.getPath(), templateDirectory.getPath(), trash.getPath()));
        if (!dryRun) {
            FileUtils.moveDirectory(componentDirectory, new File(trash, "java"));
            FileUtils.moveDirectory(templateDirectory, new File(trash, "ftl"));
        }

        // export current projects hst conf to .scaffold/.trash/date
        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

        File hstConfFile = new File(trash, projectName+"_hst.xml");
        log.info(String.format("Export hst \"%s\" config %s", projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }

        log.info("Restore backup");

        // find latest backup folder
        File backups = new File(scaffoldDir, "history");

        File[] files = backups.listFiles();
        Arrays.sort(files);

        File latest = files[files.length-1];

        // copy java and templates from backup folder
        File latestJavaFilesBackup = new File(latest, "java");
        File latestTemplateFilesBackup = new File(latest, "ftl");

        log.info(String.format("Copy backup: java components %s into %s", latestJavaFilesBackup.getPath(),  componentDirectory.getPath()));
        log.info(String.format("Copy backup: templates %s into %s", latestTemplateFilesBackup.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(latestJavaFilesBackup, componentDirectory);
            FileUtils.copyDirectory(latestTemplateFilesBackup, templateDirectory);
        }

        // restore hst config
        hstConfFile = new File(latest, projectName+"_hst.xml");
        log.info(String.format("Import hst \"%s\" config %s", projectName, hstConfFile.getPath()));
        if (!dryRun) {
            projectHstConfRoot.getSession().removeItem(projectHstConfRoot.getPath());

            Node hstConfigurations = hstRoot.getNode("hst:configurations");
            InputStream in = new BufferedInputStream(new FileInputStream(hstConfFile));
            projectHstConfRoot.getSession().importXML(hstConfigurations.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }

        // remove backup folder
        log.info(String.format("Delete backup %s", latest));
        if (!dryRun) {
            FileUtils.deleteDirectory(latest);
        }
    }

}
