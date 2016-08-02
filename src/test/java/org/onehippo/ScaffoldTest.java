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
        assertTrue(StringUtils.isEmpty(homeRoute.getContentPath()));

        Route.Component page = homeRoute.getPage();
        Route.Component main = page.getComponent("main");

        // assertTrue(main.isInconsistent()); // todo

        Route.Component container = main.getComponent("container");
        assertTrue(container.isReference());

        Route.Component carousel = container.getComponent("carousel");
        assertTrue(carousel != null);
        // assertTrue(carousel.isInconsistent()); // todo

        Route.Component featuredProducts = container.getComponent("featured-products");
        assertTrue(featuredProducts != null);
        // assertTrue(featuredProducts.isInconsistent()); // todo
    }

//    public void testContentlistRouteFold() throws RepositoryException, IOException {
//        Node hst = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst");
//
//        FileFolder scafFolder = new FileFolder(hst);
//        Route route = scafFolder.foldRoute(hst.getNode("gogreen").getNode("hst:sitemap").getNode("content"));
//
//        assertTrue("/content".equals(route.getUrl()));
//        assertTrue(StringUtils.isEmpty(route.getContentPath()));
//
//        testContentListRoute(route);
//    }
//
//    private void testContentListRoute(Route route) {
//        Route.Component page = route.getPage();
//        Route.Component menu = page.getComponents().get(0);
//
//        assertTrue(menu.isInconsistent());
//
//        Route.Component top = page.getComponents().get(1);
//        assertTrue(!top.isInconsistent());
//        assertTrue(top.isReference());
//
//        Route.Component main = page.getComponents().get(2);
//        assertTrue(!main.isInconsistent());
//
//        Route.Component contentlist = main.getComponents().get(0);
//        assertTrue(contentlist.isInconsistent());
//        assertTrue(contentlist.isReference());
//
//        Route.Component footer = page.getComponents().get(3);
//        assertTrue(footer.isInconsistent());
//    }
//
//    public void testScaffold() throws RepositoryException, IOException {
//        HSTScaffold.instance("./myhippoproject");
//
//        Node hst = JcrMockUp.mockJcrNode("/cafebabe-gogreen.xml").getNode("hst:hst");
//        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
//
//        FileFolder scafFolder = new FileFolder(hst);
//        scaffold.setScafFolder(scafFolder);
//
//        scaffold.scaffold(new File(projectDir, "foldtest.hst"), false);
//
//        List<Route> routes = scafFolder.getFold();
//
//        testHomeRoute(routes.get(0));
//        testContentListRoute(routes.get(7));
//    }
//
//    public void testRoutesWriter() {
//        // todo
//    }

}
