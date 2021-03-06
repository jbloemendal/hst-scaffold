package org.onehippo.build;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;
import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepositoryBuilder implements ScaffoldBuilder {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private Node hstRoot;
    private Node projectHstConfRoot;
    private File projectDir;
    private File scaffoldDir;
    private Map<String, String> references = new HashMap<String, String>();

    private Rollback rollback;
    private TemplateBuilder templateBuilder;

    public Pattern PATH_SEGMENT = Pattern.compile("[^/]+/");

    public RepositoryBuilder(Node hstRoot) throws RepositoryException, IOException {
        this.hstRoot = hstRoot;

        String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        projectHstConfRoot = hstRoot.getNode("hst:configurations").getNode(projectHstNodeName);

        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
        if (projectDir.exists()) {
            scaffoldDir = new File(projectDir, ".scaffold");
            if (!scaffoldDir.exists()) {
                scaffoldDir.mkdirs();
            }

            rollback = new ProjectRollback(hstRoot);
            templateBuilder = new TemplateBuilder();
        } else {
            throw new IOException(String.format("Project directory doesn't exist %s.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }

    }

    public void build(boolean dryRun) throws IOException, RepositoryException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        for (Route route : scaffold.getRoutes()) {
            try {
                buildPages(route, dryRun);
                buildSitemapItem(route, dryRun);
                buildContent(route, dryRun);
                buildMenu(route, dryRun);
            } catch (IOException e) {
                log.error("Error building route.", e);
            } catch (RepositoryException e) {
                log.error("Error building route.", e);
            }
        }

    }

    private Node buildWorkspaceComponentHstConf(Route.Component component, boolean dryRun) throws RepositoryException {
        if (component.isLeaf()) {
            if (projectHstConfRoot.hasNode("hst:workspace")) {
                Node workspace = projectHstConfRoot.getNode("hst:workspace");

                if (workspace.hasNode("hst:containers")) {
                    log.debug(String.format("%s Build workspace component %s.", (dryRun? "DRYRUN " : ""), component.getComponentPath()));
                    if (dryRun) {
                        return null;
                    }

                    Node containers = workspace.getNode("hst:containers");

                    // find container parent
                    List<Route.Component> parents = component.getParents();
                    Route.Component page = parents.remove(0);

                    // root
                    Node container;
                    if (containers.hasNode(page.getName())) {
                        container = containers.getNode(page.getName());
                    } else {
                        container = containers.addNode(page.getName(), "hst:containercomponentfolder");
                    }

                    for (Route.Component branch : component.getReferenceBranch()) {
                        if (container.hasNode(branch.getName())) {
                            container = container.getNode(branch.getName());
                        } else {
                            container = container.addNode(branch.getName(), "hst:containercomponent");
                            container.setProperty("hst:xtype", "HST.vBox");
                        }
                    }

                    if (container.hasNode(component.getName())) {
                        return container.getNode(component.getName());
                    }

                    // structure
                    if (!container.isNodeType("hst:containercomponent")) {
                        container = container.addNode(component.getName(), "hst:containercomponent");
                        container.setProperty("hst:xtype", "HST.vBox");
                    }

                    Node containerItem = container.addNode(component.getName(), "hst:containeritemcomponent");
                    containerItem.setProperty("hst:componentclassname", component.getJavaClass());
                    containerItem.setProperty("hst:template", component.getTemplateName());
                    containerItem.setProperty("hst:xtype", "HST.Item");

                    return containerItem;
                } else {
                    throw new RepositoryException("HST configuration child hst:containers misses.");
                }
            } else {
                throw new RepositoryException("HST configuration child hst:workspace misses.");
            }
        } else {
            log.error("Component is not a leaf");
            return null;
        }
    }

    private void buildPages(Route route, boolean dryRun) throws IOException, RepositoryException {
        log.debug(String.format("%s Build page %s", (dryRun? "DRYRUN " : ""), route.getPageConstruct()));

        Node pages = projectHstConfRoot.getNode("hst:pages");
        buildPageNode(pages, route.getPage(), dryRun);
    }

    private void buildPageNode(Node root, Route.Component component, boolean dryRun) throws RepositoryException, IOException {
        log.info(String.format("%s Build Page Node %s component %s", dryRun? "DRYRUN " : "", root != null? root.getPath() : "", component.getName()));

        if (component.isReference() && component.getParent() != null) { // common workspace references
            buildWorkspaceComponent(component, dryRun);
            // todo referencePath
            String referencePath = component.getPage().getName()+"/"+component.getName();
            addPageComponentReference(root, component.getName(), referencePath, dryRun);
            references.put(component.getName(), referencePath);
        } else if (component.isPointer()) { // pointer
            String referencePath = references.get(component.getName());
            addPageComponentReference(root, component.getName(), referencePath, dryRun);
        } else { // page component
            Node newComponentNode = addPageComponentNode(root, component, dryRun);

            for (Route.Component childComponent : component.getComponents()) {
                buildPageNode(newComponentNode, childComponent, dryRun);
            }
        }
    }

    private Node addPageComponentReference(Node root, String name, String referencePath, boolean dryRun) throws RepositoryException {
        log.info(String.format("%s Adding page component reference %s->%s", dryRun? "DRYRUN" : "", name, referencePath));
        if (dryRun) {
            return null;
        }

        Node componentReference;
        if (root.hasNode(name)) {
            componentReference = root.getNode(name);
        } else {
            componentReference = root.addNode(name, "hst:containercomponentreference");
            componentReference.setProperty("hst:referencecomponent", referencePath);
        }

        return componentReference;
    }

    private Node addPageComponentNode(Node root, Route.Component component, boolean dryRun) throws RepositoryException, IOException {
        // templates are unique, reuse templates
        log.debug(String.format("%s Build %s template", (dryRun? "DRYRUN " : ""), component.getName()));
        if (addHstTemplateConfig(component, dryRun)) {
            templateBuilder.buildTemplateFtlFile(component, dryRun);
        }

        templateBuilder.buildComponentJavaFile(component, dryRun);

        if (dryRun) {
            return null;
        }
        Node newComponent = null;

        if (root.hasNode(component.getName())) {
            newComponent = root.getNode(component.getName());
        } else {

            newComponent = root.addNode(component.getName(), "hst:component");
            newComponent.addMixin("hst:descriptive");
            newComponent.addMixin("mix:referenceable");
        }

        newComponent.setProperty("hst:template", component.getTemplateName());
        newComponent.setProperty("hst:componentclassname", component.getJavaClass());

        return newComponent;
    }

    private boolean addHstTemplateConfig(Route.Component component, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:templates")) {
            log.error("Project hst template node is missing.");
        }

        Node templates = projectHstConfRoot.getNode("hst:templates");
        if (!templates.hasNode(component.getName())) {
            if (dryRun) {
                return true;
            }
            Node newTemplate = templates.addNode(component.getName(), "hst:template");
            newTemplate.setProperty("hst:renderpath", component.getWebfilePath());
            return true;
        }
        return false;
    }

    private void buildWorkspaceComponent(Route.Component component, boolean dryRun) throws IOException, RepositoryException {
        log.debug(String.format("%s Build %s java file", (dryRun? "DRYRUN " : ""), component.getName()));
        templateBuilder.buildComponentJavaFile(component, dryRun);

        log.debug(String.format("%s Build %s template", (dryRun? "DRYRUN " : ""), component.getName()));
        templateBuilder.buildTemplateFtlFile(component, dryRun);
        addHstTemplateConfig(component, dryRun);

        log.debug(String.format("%s Build %s hst conf", (dryRun? "DRYRUN " : ""), component.getName()));

        if (component.isLeaf()) {
            buildWorkspaceComponentHstConf(component, dryRun);
        }

        for (Route.Component childComponent : component.getComponents()) {
            buildWorkspaceComponent(childComponent, dryRun);
        }
    }

    private void buildContent(Route route, boolean dryRun) throws RepositoryException {
        log.info(String.format("%s Build content structure %s", (dryRun? "DRYRUN" : ""), route.getContentPath()));
        if (dryRun) {
            return;
        }
        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        Node documents = projectHstConfRoot.getSession().getRootNode().getNode("content").getNode("documents");

        if (!documents.hasNode(projectName)) {
            throw new RepositoryException(String.format("Project node is missing %s.", documents.getPath()+"/"+projectName));
        }

        Matcher matcher = PATH_SEGMENT.matcher(route.getContentPath().substring(1));
        Node folderRoot = documents.getNode(projectName);
        while (matcher.find()) {
            String folderName = matcher.group().replaceAll("/", "");
            if (folderName.contains(":")) {
                break;
            }
            if (!folderRoot.hasNode(folderName)) {
                folderRoot = folderRoot.addNode(folderName, "hippostd:folder");
                folderRoot.setProperty("hippostd:foldertype", new String[]{"new-translated-folder", "new-document"});
                folderRoot.addMixin("mix:referenceable");
                log.info(String.format("Added content structure %s", folderRoot.getPath()));
            } else {
                folderRoot = folderRoot.getNode(folderName);
            }
        }
    }

    private void buildSitemapItem(Route route, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:sitemap")) {
            throw new RepositoryException("HST configuration child hst:sitemap misses.");
        }

        Node sitemapItem = projectHstConfRoot.getNode("hst:sitemap");

        String contentPath = route.getContentPath();
        int index = 1;
        for (Route.Parameter param : route.getParameters()) {
            contentPath = contentPath.replace(param.name+":"+param.type, "${"+index+"}");
            index++;
        }

        log.info(String.format("%s build sitemap item %s", (dryRun? "DRYRUN" : ""), route.getUrl()));
        if (dryRun) {
            return;
        }

        if ("/".equals(StringUtils.trim(route.getUrl()))) {
            Node newSitemapItem = addNode(sitemapItem, "root", "hst:sitemapitem", dryRun);
            newSitemapItem.setProperty("hst:componentconfigurationid", "hst:pages/" + route.getPage().getName());
            // todo algorithm to create contentPaths, url structure and content path may vary
            newSitemapItem.setProperty("hst:relativecontentpath", contentPath.substring(1));
            return;
        }

        Scanner scanner = new Scanner(route.getUrl());
        scanner.useDelimiter(Pattern.compile("/"));

        if (sitemapItem == null) {
            log.error(String.format("Sitemap items missing"));
            return;
        }

        while (scanner.hasNext()) {
            String path = scanner.next();

            if (path.startsWith(":")) {
                sitemapItem = addNode(sitemapItem, "_default_", "hst:sitemapitem", dryRun);
            } else if (path.startsWith("*")) {
                sitemapItem = addNode(sitemapItem, "_any_", "hst:sitemapitem", dryRun);
            } else {
                sitemapItem = addNode(sitemapItem, path, "hst:sitemapitem", dryRun);
            }

            sitemapItem.setProperty("hst:componentconfigurationid", "hst:pages/" + route.getPage().getName());
            // todo algorithm to create contentPaths, url structure and content path may vary
            sitemapItem.setProperty("hst:relativecontentpath", contentPath.substring(1));
        }

    }

    private void buildMenu(Route route, boolean dryRun) throws RepositoryException {
        if (!projectHstConfRoot.hasNode("hst:sitemenus")) {
            throw new RepositoryException("HST configuration child hst:sitemenus misses.");
        }
        if (route.getParameters().size() > 0) {
            return;
        }

        Node menuItems = projectHstConfRoot.getNode("hst:sitemenus");
        if (!menuItems.hasNode("main")) {
            menuItems.addNode("main", "hst:sitemenu");
        }

        Node main = menuItems.getNode("main");

        String itemName = "";
        String reference = "";

        if (route.getUrl().equals("/")) {
            itemName = "home";
            reference = "root";
        } else {
            String[] names = route.getUrl().split("/");
            if (names.length == 0) {
                return;
            }
            itemName = names[names.length - 1];
            if (StringUtils.isEmpty(itemName)) {
                return;
            }
            reference = route.getUrl().substring(1);
        }

        if (main.hasNode(itemName)) {
            return;
        }

        log.info(String.format("%s build menu item %s", (dryRun? "DRYRUN" : ""), itemName));
        if (dryRun) {
            return;
        }

        Node newMenuItem = main.addNode(itemName, "hst:sitemenuitem");
        newMenuItem.setProperty("hst:referencesitemapitem", reference);
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
