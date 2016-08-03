package org.onehippo;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.build.RepositoryBuilder;
import org.onehippo.fold.FileFolder;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ScaffoldTest extends TestCase {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);
    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ScaffoldTest(String testName) throws IOException {
        super(testName);

        projectDir = new File("./myhippoproject");
    }

    public void testHomeRouteFold() throws RepositoryException, IOException {
        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");

        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);
        Route homeRoute = scafFolder.foldRoute(gogreenHstConfRoot.getNode("hst:sitemap").getNode("root"));

        testHomeRoute(homeRoute);
    }

    private void testHomeRoute(Route homeRoute) {
        assertTrue("/".equals(homeRoute.getUrl()));
        assertTrue("/".equals(homeRoute.getContentPath()));

        Route.Component page = homeRoute.getPage();
        assertTrue(!page.isInconsistent());

        Route.Component main = page.getComponent("main");

        assertTrue(main.isInconsistent());

        Route.Component container = main.getComponent("container");
        assertTrue(!container.isInconsistent());
        assertTrue(container.isReference());

        Route.Component carousel = container.getComponent("carousel");
        assertTrue(carousel != null);
        assertTrue(carousel.isInconsistent());

        Route.Component featuredProducts = container.getComponent("featured-products");
        assertTrue(featuredProducts != null);
        assertTrue(featuredProducts.isInconsistent());
    }

    public void testContentlistRouteFold() throws RepositoryException, IOException {
        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");

        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);
        Route route = scafFolder.foldRoute(gogreenHstConfRoot.getNode("hst:sitemap").getNode("content"));

        assertTrue("/content".equals(route.getUrl()));

        testContentListRoute(route);
    }

    private void testContentListRoute(Route route) {
        Route.Component page = route.getPage();
        Route.Component menu = page.getComponent("menu");

        assertTrue(menu.isInconsistent());

        Route.Component top = page.getComponent("top");
        assertTrue(!top.isInconsistent());
        assertTrue(top.isReference());

        Route.Component main = page.getComponent("main");
        assertTrue(main.isReference());
        assertTrue(!main.isInconsistent());

        Route.Component contentlist = main.getComponent("contentlist");
        assertTrue(contentlist.isInconsistent());

        Route.Component footer = page.getComponent("footer");
        assertTrue(footer.isInconsistent());
    }

    public void testScaffold() throws RepositoryException, IOException {
        HSTScaffold.instance("./myhippoproject");

        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");
        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);
        scaffold.setScafFolder(scafFolder);

        List<Route> routes = scaffold.scaffold(new File(projectDir, "fold-test.hst"), false);

        testHomeRoute(routes.get(0));
        testContentListRoute(routes.get(5));
    }

    // todo add tests for sitemap items placed in workspace, commons...
    // todo add tests for pages placed in workspace, commons...

}
