package org.onehippo;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

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

        String projectHstNodeName = HSTScaffold.properties.getProperty("projectHstNodeName");
        projectHstConfRoot = hstRoot.getNode("hst:configurations").getNode(projectHstNodeName);

        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
        if (!projectDir.exists()) {
            throw new IOException(String.format("Project directory doesn't exist $1.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }

        scaffoldDir = new File(projectDir, ".scaffold");
        scaffoldDir.mkdir();
    }

    private void backup() throws IOException, RepositoryException {
        File backup = new File(scaffoldDir, ""+System.currentTimeMillis());
        backup.mkdir();

        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_HST_NODE_NAME);

        OutputStream out = new FileOutputStream(new File(backup, projectName+"_hst.xml"));
        projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);

        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.DEFAULT_TEMPLATE_PATH);
        FileUtils.copyDirectory(new File(projectDir, javaFilePath), new File(backup, "java"));
        FileUtils.copyDirectory(new File(projectDir, ftlFilePath), new File(backup, "ftl"));
    }

    public void dryRun() throws RepositoryException, IOException {
        build(true);
    }

    public void build() throws IOException, RepositoryException {
        // todo backup();
        build(false);
    }

    private void buildComponentJavaFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(this.getClass().getResource("/Component.java.mustache").getFile());

        Writer writer= new PrintWriter(System.out);;
        if (!dryRun) {
            File javaClassFile = new File(component.getPathJavaClass());
            if (javaClassFile.exists()) {
                log.info(String.format("Java component file $1 alreday exists", javaClassFile.getPath()));
            } else {
                writer = new FileWriter(javaClassFile);
            }
        }

        mustache.execute(writer, new HashMap<String, String>() {
            {
                put("projectPackage", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_PACKAGE_NAME));
                put("name", StringUtils.capitalize(component.getName()));
            }
        }).flush();
    }

    private void buildTemplateFtlFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(this.getClass().getResource("/template.ftl.mustache").getFile());

        Writer writer = new PrintWriter(System.out);
        if (!dryRun) {
            File templateFile = new File(component.getTemplateFilePath());
            if (templateFile.exists()) {
                log.info(String.format("Template file $1 already exists.", templateFile.getPath()));
            } else {
                writer = new FileWriter(templateFile);
            }
        }

        mustache.execute(writer, new HashMap<String, Object>() {
            {
                put("childs", component.getComponents());
            }
        }).flush();
    }

    private void buildComponentHstConf(Route.Component component, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:components")) {
            throw new RepositoryException("HST configuration child hst:components misses.");
        }

        Node components = projectHstConfRoot.getNode("hst:components");
        String nodeName = component.getName();
        int index = 1;
        while (components.hasNode(nodeName)) {
            nodeName = component.getName()+"_"+index;
        }

        log.info(String.format("New component node $1", components.getPath()+"/"+nodeName+"."));
        if (!dryRun) {
            Node newComponent = components.addNode(nodeName);
            newComponent.setProperty("hst:componentclassname", component.getJavaClass());
            newComponent.setProperty("hst:template", component.getTemplateName());
        }
    }

    private void buildComponent(Route.Component component, boolean dryRun) throws IOException, RepositoryException {
        buildComponentJavaFile(component, dryRun);
        buildTemplateFtlFile(component, dryRun);
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

    private void build(boolean dryRun) {
        HSTScaffold scaffold = HSTScaffold.instance();
        for (Route route : scaffold.getRoutes()) {
            try {
                buildComponent(route.getPage(), true);
                buildSitemapItem(route, true);
            } catch (IOException e) {
                log.error("Error building route.", e);
            } catch (RepositoryException e) {
                log.error("Error building route.", e);
            }
        }
    }

    public void rollback() {
        // todo rollback latest build
    }

}
