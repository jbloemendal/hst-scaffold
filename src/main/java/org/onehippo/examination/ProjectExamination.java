package org.onehippo.examination;


import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;
import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// TODO
public class ProjectExamination implements Examination {

    final static Logger log = Logger.getLogger(ProjectExamination.class);

    private Node hstRoot;
    private Node projectHstConfRoot;
    private File projectDir;
    private File scaffoldDir;
    private Map<String, String> references = new HashMap<String, String>();

    public Pattern PATH_SEGMENT = Pattern.compile("[^/]+/");

    public ProjectExamination(Node hstRoot) throws RepositoryException, IOException {
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

    // TODO adapt
    private Node diagnoseHstConfig(Route.Component component, boolean dryRun) throws RepositoryException {
        if (!component.isLeaf()) {
            log.error("Component is not a leaf");
            return null;
        }

        if (!projectHstConfRoot.hasNode("hst:workspace")) {
            throw new RepositoryException("HST configuration child hst:workspace misses.");
        }

        Node workspace = projectHstConfRoot.getNode("hst:workspace");
        if (!workspace.hasNode("hst:containers")) {
            throw new RepositoryException("HST configuration child hst:containers misses.");
        }

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
        if (!containers.hasNode(page.getName())) {
            container = containers.addNode(page.getName(), "hst:containercomponentfolder");
        } else {
            container = containers.getNode(page.getName());
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

        // let's create structure, if missing
        if (!container.isNodeType("hst:containercomponent")) {
            container = container.addNode(component.getName(), "hst:containercomponent");
            container.setProperty("hst:xtype", "HST.vBox");
        }

        Node containerItem = container.addNode(component.getName(), "hst:containeritemcomponent");
        containerItem.setProperty("hst:componentclassname", component.getJavaClass());
        containerItem.setProperty("hst:template", component.getTemplateName());
        containerItem.setProperty("hst:xtype", "HST.Item");

        return containerItem;
    }

    public Map<String, Object> diagnoseComponents() {
        return null;
    }

    public Map<String, Object> diagnoseSitemaps() {
        return null;
    }

    public Map<String, Object> diagnoseMenus() {
        return null;
    }

    public Map<String, Object> diagnoseTemplates() {
        return null;
    }
}
