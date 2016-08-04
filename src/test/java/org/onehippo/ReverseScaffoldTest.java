package org.onehippo;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.onehippo.build.RepositoryBuilder;
import org.onehippo.fold.FileFolder;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ReverseScaffoldTest extends TestCase {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);
    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ReverseScaffoldTest(String testName) throws IOException {
        super(testName);

        projectDir = new File("./myhippoproject");
    }

    public void testHomeRouteFold() throws RepositoryException, IOException {
        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");

        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);

        Node sitemapItem = gogreenHstConfRoot.getNode("hst:sitemap").getNode("root");
        String relativeComponentPath = sitemapItem.getProperty("hst:componentconfigurationid").getString();

        Node pageNode = scafFolder.getRelativeNode(gogreenHstConfRoot, relativeComponentPath);
        Route.Component page = scafFolder.fold(pageNode);

        testHomePage(page);
    }

    private void testHomePage(Route.Component page) {
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
        Node sitemapItem = gogreenHstConfRoot.getNode("hst:sitemap").getNode("content");
        String relativeComponentPath = sitemapItem.getProperty("hst:componentconfigurationid").getString();

        Node pageNode = scafFolder.getRelativeNode(gogreenHstConfRoot, relativeComponentPath);
        Route.Component page = scafFolder.fold(pageNode);

        testContentListPage(page);
    }

    private void testContentListPage(Route.Component page) {
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
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");
        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);
        scaffold.setScafFolder(scafFolder);

        Map<String, Route> routes = scaffold.scaffold(new File(projectDir, "fold-test.hst"), false);

        testHomePage(routes.get("/").getPage());
        testContentListPage(routes.get("/content").getPage());
    }

    public void testContentPlaceHolders() throws RepositoryException, IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        Node gogreenHstConfRoot = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst").getNode("hst:configurations").getNode("gogreen");
        FileFolder scafFolder = new FileFolder(gogreenHstConfRoot);
        scaffold.setScafFolder(scafFolder);

        Map<String, Route> routes = scaffold.scaffold(new File(projectDir, "fold-test.hst"), false);

        Route contentDetail = routes.get("/content/_any_.html");
        assertEquals(contentDetail.getContentPath(), "/content/param1:String");

        Route newsDetail = routes.get("/news/:param1");
        assertEquals(newsDetail.getContentPath(), "/newsfacets/param1:String");
    }

    // todo add pointer test (main)

    // todo add tests for sitemap items placed in workspace, commons...

    // todo add tests for pages placed in workspace, commons...

}
