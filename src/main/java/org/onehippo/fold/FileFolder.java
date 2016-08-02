package org.onehippo.fold;

import org.apache.log4j.Logger;
import org.onehippo.Route;
import org.onehippo.build.RepositoryBuilder;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (!componentNode.isNodeType("hst:component")
                && !componentNode.isNodeType("hst:containercomponentreference")
                && !componentNode.isNodeType("hst:containeritemcomponent")) {
            return null;
        }

        Route.Component component = new Route.Component(componentNode.getName());

        if (componentNode.hasProperty("hst:referencecomponent")) {
            String reference = componentNode.getProperty("hst:referencecomponent").getString();
            if (references.containsKey(reference)) {
                component.setPointer(true);
                Route.Component referenced = references.get(reference);
                component.add(referenced.getComponents());
            } else {
                component.setReference(true);

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

                addChilds(referenced, component);
                references.put(reference, component);
            }
        }


        addChilds(componentNode, component);

        return component;
    }

    private void addChilds(Node componentNode, Route.Component component) throws RepositoryException {
        NodeIterator iterator = componentNode.getNodes();

        while (iterator.hasNext()) {
            Node child = iterator.nextNode();
            if (child == null) {
                continue;
            }
            Route.Component childComponent = fold(child);
            if (childComponent != null) {
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
        }
        return contentPath;
    }

    private StringBuilder getUrl(Node sitemapItem) throws RepositoryException {
        StringBuilder url = new StringBuilder();
        Node cursor = sitemapItem;
        int paramIndex = 0;
        while (!"hst:sitemap".equals(cursor.getName())) {
            if (url.length() != 0) {
                url.insert(0, "/");
            }
            if ("_default_".equals(cursor.getName())) {
                url.insert(0, ":param"+paramIndex);
                paramIndex++;
            } else if ("_any_".equals(cursor.getName())) {
                url.insert(0, ":param" + paramIndex);
                paramIndex++;
            } else if ("root".equals(cursor.getName())) {
                url.insert(0, "/");
            } else {
                url.insert(0, cursor.getName());
            }
            cursor = cursor.getParent();
        }
        return url;
    }

    public List<Route> getFold() throws RepositoryException {
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

    public void fold(File destination, boolean dryRun) {
        // todo
    }

}
