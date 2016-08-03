package org.onehippo.fold;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;
import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// EXPERIMENTAL!
public class FileFolder implements Folder {

    final static Logger log = Logger.getLogger(FileFolder.class);

    private Node hstProjectRoot;

    private Pattern CONTENTPATH_PLACEHOLDER = Pattern.compile("\\$\\{([^\\}]+)\\}");
    private Pattern NUMBER = Pattern.compile("[0-9]+");

    private Map<String, Route.Component> references = new HashMap<String, Route.Component>();

    public FileFolder(Node hstProjectRoot) {
        this.hstProjectRoot = hstProjectRoot;
    }

    private Route.Component fold(Node componentNode) throws RepositoryException {
        Route.Component component = new Route.Component(componentNode.getName());

        boolean inconsistent = isInconsistent(componentNode, component);
        component.setInconsistent(inconsistent);

        if (componentNode.hasProperty("hst:referencecomponent")) {
            include(componentNode, component);
        }

        addChilds(componentNode, component);

        return component;
    }

    private boolean isInconsistent(Node componentNode, Route.Component component) throws RepositoryException {
        if (componentNode.hasProperty("hst:componentclassname")) {
            String className = componentNode.getProperty("hst:componentclassname").getString();
            if (StringUtils.isEmpty(className)) {
                log.warn(String.format("Component %s is inconsistent JavaClass %s misses", component.getName(), className));
                return true;
            }
            String classPath = className.replace(".", "/");
            String filePath = HSTScaffold.properties.get(HSTScaffold.JAVA_COMPONENT_PATH)+"/"+classPath;
            File javaFile = new File(filePath);
            if (!javaFile.exists()) {
                log.warn(String.format("Component %s is inconsistent Java File %s misses", component.getName(), javaFile.getPath()));
                return true;
            }
        }
        if (componentNode.hasProperty("hst:template")) {
            String templateName = componentNode.getProperty("hst:template").getString();
            if (StringUtils.isEmpty(templateName)) {
                log.warn(String.format("Component %s is inconsistent template property is empty.", component.getName(), templateName));
                return true;
            }
            Node templates = hstProjectRoot.getNode("hst:templates");
            if (!templates.hasNode(templateName)) {
                log.warn(String.format("Component %s is inconsistent template node %s is missing.", component.getName(), templateName));
                return true;
            }

            Node templateNode = templates.getNode(templateName);
            if (templateNode.hasProperty("hst:renderpath")) {
                String renderPath = templateNode.getProperty("hst:renderpath").getString();
                if (!component.getWebfilePath().equals(renderPath)) {
                    log.warn(String.format("Component %s is inconsistent %s != %s", component.getName(), renderPath, component.getWebfilePath()));
                    return true;
                }
            }
            File templateFile = new File(component.getTemplateFilePath());
            if (!templateFile.exists()) {
                log.warn(String.format("Component %s is inconsistent template file %s is missing.", component.getName(), templateFile.getPath()));
                return true;
            }
        }
        return false;
    }

    private void include(Node componentNode, Route.Component component) throws RepositoryException {
        // TODO lookup pages from common, workspace, etc ...

        String reference = componentNode.getProperty("hst:referencecomponent").getString();

        if (references.containsKey(reference)) {
            Route.Component referenced = references.get(reference);
            component.add(referenced.getComponents());
        } else {
            Node referenceBase;
            if (componentNode.isNodeType("hst:containercomponentreference")) {
                referenceBase = getNode(hstProjectRoot, "hst:workspace/hst:containers");
            } else {
                referenceBase = hstProjectRoot;
            }

            Node referenced = getNode(referenceBase, reference);
            if (referenced == null) {
                log.error(String.format("Referenced node %s doesn't exist.", reference));
            }

            Route.Component referencedComponent = fold(referenced);
            List<Route.Component> components = referencedComponent.getComponents();
            if (reference.startsWith("hst:abstractpages") || reference.startsWith("hst:pages")) {
                for (Route.Component include : components) {
                    include.setInherited(true);
                }
            } else {
                component.setReference(true);
            }

            component.add(components);

            references.put(reference, component);
        }
    }

    private void addChilds(Node componentNode, Route.Component component) throws RepositoryException {
        NodeIterator iterator = componentNode.getNodes();

        // todo order childs by template include order

        while (iterator.hasNext()) {
            Node child = iterator.nextNode();
            if (child == null) {
                continue;
            }
            Route.Component childComponent = fold(child);
            if (childComponent != null) {
                if (component.has(childComponent.getName())) {
                    log.info(String.format("component %s overwrite child %s", component.toString(), childComponent.toString()));
                    component.remove(childComponent.getName());
                }

                component.add(childComponent);
            }
        }
    }

    private String getPageConstruct(Route.Component page) {
        return "";
    }

    public Route foldRoute(Node sitemapItem) throws RepositoryException {
        if (!sitemapItem.hasProperty("hst:componentconfigurationid")) {
            return null;
        }

        StringBuilder url = getUrl(sitemapItem);
        String contentPath = getContentPath(sitemapItem);

        String relativeComponentPath = sitemapItem.getProperty("hst:componentconfigurationid").getString();

        Route.Component page = fold(getNode(hstProjectRoot, relativeComponentPath));

        Route route = new Route(url.toString(), contentPath, getPageConstruct(page));
        route.setPage(page);

        return route;
    }

    private Node getNode(Node node, String relativePath) throws RepositoryException {
        Node cursor = node;
        for (String path : relativePath.split("/")) {
            if (cursor.hasNode(path)) {
                cursor = cursor.getNode(path);
            } else {
                return null;
            }
        }
        return cursor;
    }

    private String getContentPath(Node sitemapItem) throws RepositoryException {
        String contentPath = "";
        if (sitemapItem.hasProperty("hst:relativecontentpath")) {
            contentPath = sitemapItem.getProperty("hst:relativecontentpath").getString();
            Matcher matcher = CONTENTPATH_PLACEHOLDER.matcher(contentPath);
            while (matcher.find()) {
                String placeHolder = matcher.group(1);
                if ("parent".equals(placeHolder)) {
                    // TODO
                } else if (NUMBER.matcher(placeHolder).matches()) {
                    // TODO
                }
            }
        } else {
            contentPath = "/";
        }
        return contentPath;
    }

    private StringBuilder getUrl(Node sitemapItem) throws RepositoryException {
        StringBuilder url = new StringBuilder();
        Node cursor = sitemapItem;
        int paramIndex = 0;
        while (!"hst:sitemap".equals(cursor.getName())) {
            if ("_default_".equals(cursor.getName())) {
                url.insert(0, ":param"+paramIndex);
                paramIndex++;
            } else if ("_any_".equals(cursor.getName())) {
                url.insert(0, ":param" + paramIndex);
                paramIndex++;
            } else if ("root".equals(cursor.getName())) {
                // nothing
            } else {
                url.insert(0, cursor.getName());
            }
            url.insert(0, "/");
            cursor = cursor.getParent();
        }

        return url;
    }

    public List<Route> fold() throws RepositoryException {
        // todo add sitemap items from workspace etc...
        List<Route> routes = new LinkedList<Route>();
        Node sitemap = hstProjectRoot.getNode("hst:sitemap");
        NodeIterator iterator = sitemap.getNodes();
        while (iterator.hasNext()) {
            Node sitemapItem = iterator.nextNode();
            if (sitemapItem == null) {
                continue;
            }
            routes.add(foldRoute(sitemapItem));
        }
        return routes;
    }

    public List<Route> fold(File destination, boolean dryRun) throws RepositoryException, IOException {
        List<Route> routes = fold();

        StringBuilder scaffold = new StringBuilder();
        scaffold.append("#URL\t\tCONTENTPATH\t\tCOMPONENTS");
        scaffold.append("\n\n");

        for (Route route : routes) {
            scaffold.append(route.toString());
            scaffold.append("\n");
        }

        if (dryRun) {
            System.out.println(scaffold.toString());
            return routes;
        }

        FileWriter writer = new FileWriter(destination);
        try {
            writer.write(scaffold.toString());
        } finally {
            writer.close();
        }

        return routes;
    }

}
