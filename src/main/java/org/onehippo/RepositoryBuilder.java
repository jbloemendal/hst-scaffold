package org.onehippo;


import javax.jcr.Node;

public class RepositoryBuilder implements ScaffoldBuilder {

    private Node hstRoot;

    public RepositoryBuilder(Node hstRoot) {
        this.hstRoot = hstRoot;
    }

    public void dryRun() {
        // todo build, don't modify files, log changes
    }

    public void build() {
        // todo create a backup of files which are changed

    }

    private void buildPage(Route.Component component, boolean dryRun) {

        // todo
        // javaclass
        // template
        // jcr hst configuration

        for (Route.Component child : component.getComponents()) {
            buildComponent(child);
        }
    }

    private void buildComponent(Route.Component component) {
        // todo
    }

    private void buildSitemapItem(Route route) {
        // todo
    }

    private void build(boolean dryRun) {
        HSTScaffold scaffold = HSTScaffold.instance();
        for (Route route : scaffold.getRoutes()) {
            buildPage(route.getPage(), dryRun);
            buildSitemapItem(route);
        }
    }

    public void rollback() {
        // todo rollback latest build
    }

}
