package org.onehippo.build;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;
import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class RepositoryBuilder implements ScaffoldBuilder {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private Node hstRoot;
    private Node projectHstConfRoot;
    private File projectDir;
    private File scaffoldDir;

    private Rollback rollback;
    private TemplateBuilder templateBuilder;

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

        rollback = new ProjectRollback(hstRoot);
        templateBuilder = new TemplateBuilder();
    }

    public void build(boolean dryRun) throws IOException, RepositoryException {
        backup(dryRun);

        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        for (Route route : scaffold.getRoutes()) {
            try {
                buildPages(route, dryRun);
                buildSitemapItem(route, dryRun);
                buildContent(route, dryRun);
            } catch (IOException e) {
                log.error("Error building route.", e);
            } catch (RepositoryException e) {
                log.error("Error building route.", e);
            }
        }

    }



    private void buildWorkspaceComponentHstConf(Route.Component component, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:components")) {
            throw new RepositoryException("HST configuration child hst:components misses.");
        }

        Node components = projectHstConfRoot.getNode("hst:components");
        String nodeName = component.getName();
        int index = 1;
        while (components.hasNode(nodeName)) { // todo, improve ???
            nodeName = component.getName()+"_"+index;
            index++;
        }

        log.info(String.format("%s New component node %s",(dryRun? "DRYRUN " : ""), components.getPath()+"/"+nodeName+"."));
        if (!dryRun) {
            Node newComponent = components.addNode(nodeName, "hst:component");
            newComponent.setProperty("hst:componentclassname", component.getJavaClass());
            newComponent.setProperty("hst:template", component.getTemplateName());
        }
    }

    private void buildPages(Route route, boolean dryRun) throws IOException, RepositoryException {
        log.debug(String.format("%s Build page %s", (dryRun? "DRYRUN " : ""), route.getPageConstruct()));

        Node pages = projectHstConfRoot.getNode("hst:pages");
        buildPageNode(route, pages, route.getPage(), dryRun);
    }

    private void buildPageNode(Route route, Node root, Route.Component component, boolean dryRun) throws RepositoryException, IOException {
        log.info(String.format("%s Build Page Node %s component %s", dryRun? "DRYRUN " : "", root != null? root.getPath() : "", component.getName()));

        if (component.isPointer()) {
            // todo
        } else if (component.isReference()) {
            // todo
            Node referenceComponent = buildWorkspaceComponent(component, dryRun);
            addPageComponentReference(root, component, dryRun);
        } else {
            Node newComponent = null;
            if (dryRun) {
                log.info(String.format("%s page %s, adding node %s", dryRun? "DRYRUN " : "", route.getPageConstruct(), component.getName()));
            } else {
                newComponent = addPageComonentNode(root, component, dryRun);
            }

            for (Route.Component child : component.getComponents()) {
                buildPageNode(route, newComponent, child, dryRun);
            }
        }
    }

    private Node addPageComponentReference(Node root, Route.Component component, boolean dryRun) throws RepositoryException {
        Node componentReference;

        if (root.hasNode(component.getName())) {
            componentReference = root.getNode(component.getName());
        } else {
            componentReference = root.addNode(component.getName(), "hst:containercomponentreference");
        }

        // todo

        return null;
    }

    private Node addPageComonentNode(Node root, Route.Component component, boolean dryRun) throws RepositoryException, IOException {
        Node newComponent;
        if (root.hasNode(component.getName())) {
            newComponent = root.getNode(component.getName());
        } else {
            newComponent = root.addNode(component.getName(), "hst:component");
            newComponent.addMixin("hst:descriptive");
            newComponent.addMixin("mix:referenceable");

            log.debug(String.format("%s Build %s template", (dryRun? "DRYRUN " : ""), component.getName()));
            templateBuilder.buildTemplateFtlFile(component, dryRun);
            addHstTemplateConfig(component);

            newComponent.setProperty("hst:template", component.getTemplateName());
        }
        return newComponent;
    }

    private void addHstTemplateConfig(Route.Component component) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:templates")) {
            log.error("Project hst template node is missing.");
        }

        Node templates = projectHstConfRoot.getNode("hst:templates");
        Node newTemplate = templates.addNode(component.getName(), "hst:template");
        newTemplate.setProperty("hst:renderpath", component.getWebfilePath());
    }

    private Node buildWorkspaceComponent(Route.Component component, boolean dryRun) throws IOException, RepositoryException {
        log.debug(String.format("%s Build %s java file", (dryRun? "DRYRUN " : ""), component.getName()));
        templateBuilder.buildComponentJavaFile(component, dryRun);

        log.debug(String.format("%s Build %s template", (dryRun? "DRYRUN " : ""), component.getName()));
        templateBuilder.buildTemplateFtlFile(component, dryRun);
        addHstTemplateConfig(component);

        log.debug(String.format("%s Build %s hst conf", (dryRun? "DRYRUN " : ""), component.getName()));
        buildWorkspaceComponentHstConf(component, dryRun);

        return null; // todo
    }

    /*
    /news/date:String/id:String
    /news
    */
    private void buildContent(Route route, boolean dryRun) throws RepositoryException {
        // todo algorithm to create contentPaths
//        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
//
//        Node documents = projectHstConfRoot.getSession().getNode("/content/documents");
//
//        String contentPath = route.getContentPath();
//        List<Route.Parameter> parameters = route.getParameters();
//
//        int index = 1;
//        for (Route.Parameter param : parameters) {
//            contentPath = contentPath.replace("/"+param.name+":"+param.type, "");
//            index++;
//        }
//
//        ValueFactory valueFactory = projectHstConfRoot.getSession().getValueFactory();
//
//        Node siteContentRoot = documents.addNode(projectName, "hippostd:folder");
//        siteContentRoot.setProperty("hippostd:foldertype", valueFactory.);
//
//        ValueFactory valueFactory=context.getSession().getValueFactory();
//        Value[] values={valueFactory.createValue("testValue")};
//        property=node.setProperty("testProperty",values);

    }

    private void buildSitemapItem(Route route, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:sitemap")) {
            throw new RepositoryException("HST configuration child hst:sitemap misses.");
        }

        Scanner scanner = new Scanner(route.getUrl());
        scanner.useDelimiter(Pattern.compile("/"));

        Node sitemapItem = projectHstConfRoot.getNode("hst:sitemap");

        if (sitemapItem == null) {
            log.error(String.format("Sitemap items missing"));
            return;
        }

        String contentPath = route.getContentPath();
        int index = 1;
        for (Route.Parameter param : route.getParameters()) {
            contentPath = contentPath.replace(param.name+":"+param.type, "${"+index+"}");
            index++;
        }

        while (scanner.hasNext()) {
            String path = scanner.next();

            if (path.startsWith(":")) {
                sitemapItem = addNode(sitemapItem, "_default_", "hst:sitemapitem", dryRun);
            } else if (path.startsWith("*")) {
                sitemapItem = addNode(sitemapItem, "_any_", "hst:sitemapitem", dryRun);
            } else if ("/".equals(path)) {
                sitemapItem = addNode(sitemapItem, "root", "hst:sitemapitem", dryRun);
            } else {
                sitemapItem = addNode(sitemapItem, path, "hst:sitemapitem", dryRun);
            }

            sitemapItem.setProperty("hst:componentconfigurationid", "hst:pages/" + route.getPage().getName());
            // todo algorithm to create contentPaths, url structure and content path may vary
            sitemapItem.setProperty("hst:relativecontentpath", contentPath.substring(1));
        }

    }

    private Node addNode(Node node, String nodeName, String type, boolean dryRun) throws RepositoryException {
        Node newNode = null;
        String path = (node != null && StringUtils.isNotEmpty(node.getPath()))? node.getPath() : "";
        log.debug(String.format("%s %s adding node %s",(dryRun?"DRYRUN": ""), path, nodeName));
        if (node.hasNode(nodeName)) {
            log.warn(String.format("Sitemap item %s/%s already exists.", node.getPath(), nodeName));
            newNode = node.getNode(nodeName);
        } else if (!dryRun) {
            newNode = node.addNode(nodeName, type);
        }
        return newNode;
    }

    public void backup(boolean dryRun) throws IOException, RepositoryException {
        rollback.backup(dryRun);
    }

    public void rollback(boolean dryRun) throws IOException, RepositoryException {
        rollback.rollback(dryRun);
    }
}
