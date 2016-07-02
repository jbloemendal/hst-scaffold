package org.onehippo;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.HashMap;

public class RepositoryBuilder implements ScaffoldBuilder {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private Node hstRoot;
    private Node projectHstConfRoot;

    public RepositoryBuilder(Node hstRoot) throws RepositoryException {
        this.hstRoot = hstRoot;
        String projectHstNodeName = HSTScaffold.properties.getProperty("projectHstNodeName");
        projectHstConfRoot = hstRoot.getNode("hst:configurations").getNode(projectHstNodeName);
    }

    public void dryRun() {
        build(true);
    }

    public void build() {
        build(false);
    }

    private void buildComponentJavaFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(this.getClass().getResource("/Component.java.mustache").getFile());

        Writer writer;
        if (dryRun) {
            writer = new PrintWriter(System.out); // log?
        } else {
            // todo if file exists, backup file
            writer = new FileWriter(new File(component.getPathJavaClass()));
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

        Writer writer;
        if (dryRun) {
            writer = new PrintWriter(System.out); // log?
        } else {
            // todo if file exists, backup file
            writer = new FileWriter(new File(component.getTemplateFilePath()));
        }

        mustache.execute(writer, new HashMap<String, Object>() {
            {
                put("childs", component.getComponents());
            }
        }).flush();
    }

    private void buildComponentHstConf(Route.Component component, boolean dryRun) throws RepositoryException {
        Node components = projectHstConfRoot.getNode("hst:components");
        // todo check same name siblings, add properties ...
        // components.addNode(component.getName());
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

    private void buildSitemapItem(Route route, boolean dryRun) {
        // todo
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
