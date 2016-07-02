package org.onehippo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Unit test for HSTScaffold.
 */
public class HSTScaffoldTest extends TestCase {

    final static Logger log = Logger.getLogger(HSTScaffold.class);
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    public static final String HST_TEMPLATE = "hst:template";
    public static final String HST_CONTAINERITEMCOMPONENT = "hst:containeritemcomponent";
    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HSTScaffoldTest(String testName) {
        super(testName);

        HSTScaffold.instance();
        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(HSTScaffoldTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public void testRoute() {
        Route route = new Route("       /text/*path ",       "    /contact/path:String ",    "  text(header,main(banner, textInner),footer)  ");

        List<Route.Parameter> parameters = route.getParameters();
        log.debug(""+parameters.get(0));
        assertTrue(parameters.get(0).name.equals("path"));

        Route.Component page = route.getPage();
        List<Route.Component> components = page.getComponents();

        assertTrue(components.size() == 3);

        Route.Component main = components.get(1);
        assertTrue(main.getComponents().size() == 2);

        Route.Component header = components.get(0);
        String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
//todo        assertTrue((projectDir+"/"+HSTScaffold.DEFAULT_TEMPLATE_PATH+"/text/header.ftl").equals(header.getTemplateFilePath()));
        assertTrue((projectDir+"/"+HSTScaffold.DEFAULT_COMPONENT_PATH+"/HeaderComponent.java").equals(header.getPathJavaClass()));
    }

    public void testScaffoldRoutes() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        assertEquals(6, routes.size());
    }

    public void testUrlParameter() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(3).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals("String", type);
    }

    public void testWildcardUrlParameter() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(5).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals("String", type);
    }

    public void testDryRun() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = HSTScaffold.instance();

        // persist data to dry run .scaffold/dryrun directory instead of project
        scaffold.dryRun();

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }


//    public void testRollback() {
//        final Map<String, String> before = TestUtils.dirHash(projectDir);
//
//        HSTScaffold scaffold = HSTScaffold.instance();
//
//        // todo backup all files which are going to be changed (versioned history, timestamp folders)
//        scaffold.build();
//
//        assertTrue(new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME).exists());
//        assertTrue(new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME +"/history").exists());
//
//        // todo backup edited files (manual changed files) (rollback - rollback)
//        // todo warn user that he edited files, which we will revert (force option?)
//        scaffold.rollback();
//
//        final Map<String, String> after = TestUtils.dirHash(projectDir);
//
//        assertFalse(TestUtils.dirChanged(before, after));
//    }


//    public void testUpdateDryRun() {
//        final Map<String, String> before = TestUtils.dirHash(projectDir);
//
//        HSTScaffold scaffold = HSTScaffold.instance();
//
//        scaffold.build();
//
//        assertTrue(new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME).exists());
//
//        scaffold.addRoute(new Route("/dryrun", "dryrun", "dryrun(header,main(banner,text),footer)"));
//
//        scaffold.dryRun();
//
//        assertTrue(dryRunUpdated); // TODO
//
//        scaffold.rollback();
//
//        final Map<String, String> after = TestUtils.dirHash(projectDir);
//        assertFalse(TestUtils.dirChanged(before, after));
//    }


    private boolean isHstTemplateConfValid(Node hstSiteCnfRoot, Route.Component component) throws RepositoryException {
      /*
        <sv:node sv:name="base-top-menu">
        <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>hst:template</sv:value>
        </sv:property>
        <sv:property sv:name="hst:renderpath" sv:type="String">
        <sv:value>webfile:/freemarker/gogreen/base-top-menu.ftl</sv:value>
        </sv:property>
        </sv:node>
        */
        Node templates = hstSiteCnfRoot.getNode("hst:templates");

        if (!templates.hasNode(component.getTemplateName())) {
            return false;
        }

        Node template = templates.getNode(component.getTemplateName());
        if (!template.getProperty("jcr:primaryType").equals("hst:template")) {
            return false;
        }
        if (!template.getProperty("hst:renderpath").equals(component.getWebfilePath())) {
            return false;
        }

        return true;
    }

    private String readFile(String fileName) throws IOException {
        Reader reader = new BufferedReader(new FileReader(new File(fileName)));

        StringBuilder templateBuilder = new StringBuilder();

        char[] buffer = new char[1024];
        while (reader.read(buffer) != -1) {
            templateBuilder.append(buffer);
        }

        return templateBuilder.toString();
    }

    private boolean isTemplateIncludesValid(Node hstSiteCnfRoot, Route.Component component) throws IOException {
        String template = readFile(component.getTemplateFilePath());
        for (Route.Component child : component.getComponents()) {
            if (template.contains("<@hst.include ref=\""+child.getName()+"\">")) {
                return false;
            }
        }
        return true;
    }



    private boolean validateComponent(Node hstSiteCnfRoot, Route.Component component) throws XPathExpressionException, RepositoryException, IOException {
        /*
        // validate
        <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>hst:containeritemcomponent</sv:value>
        </sv:property>
        <sv:property sv:name="hst:componentclassname" sv:type="String">
        <sv:value>org.onehippo.cms7.essentials.components.EssentialsCarouselComponent</sv:value>
        </sv:property>
        <sv:property sv:name="hst:template" sv:type="String">
        <sv:value>essentials-carousel</sv:value>
        </sv:property>
        <sv:property sv:name="hst:xtype" sv:type="String">
        <sv:value>HST.Item</sv:value>
        </sv:property>
        */

        Node componentNode = hstSiteCnfRoot.getNode(component.getComponentPath());
        if (!isHstComponentConfValid(component, componentNode)
                || !isHstTemplateConfValid(hstSiteCnfRoot, component)
                || !isTemplateIncludesValid(hstSiteCnfRoot, component)) {
            return false;
        }


        for (Route.Component child : component.getComponents()) {
            if (!componentNode.hasNode(child.getName())) {
                return false;
            }

            if (!validateComponent(hstSiteCnfRoot, child)) {
                return false;
            }
        }

        return true;
    }

    private boolean isHstComponentConfValid(Route.Component component, Node componentNode) throws RepositoryException {
        if (componentNode == null) {
            return false;
        }

        if (!componentNode.hasProperty(JCR_PRIMARY_TYPE)
                || !componentNode.getProperty(JCR_PRIMARY_TYPE).equals(HST_CONTAINERITEMCOMPONENT)) {
            return false;
        }
        if (!componentNode.hasProperty(HST_COMPONENTCLASSNAME)
                || !component.getJavaClass().equals(componentNode.getProperty(HST_COMPONENTCLASSNAME).getValue().getString())) {
            return false;
        }

        File javaFile = new File(component.getPathJavaClass());
        assertTrue(javaFile.exists());

        if (!componentNode.hasProperty(HST_TEMPLATE)
                || !component.getTemplateFilePath().equals(componentNode.getProperty(HST_TEMPLATE).getValue().getString())) {
            return false;
        }

        return true;
    }


    public void testRoutes() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance();

            Node hst = JcrMockUp.mockJcrNode("/hst.xml");

            scaffold.setBuilder(new RepositoryBuilder(hst));
            scaffold.build();

            String projectHstNodeName = HSTScaffold.properties.getProperty("projectHstNodeName");

            Node components = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:components");
            Node sitemap = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:sitemap");

            for (Route route : scaffold.getRoutes()) {
                // todo
                // validateSitemap(sitemap, route);
                // validateComponent(components, route.getPage());
            }
        } catch (Exception e) {
            log.error("Error testing components, XPath expression", e);
        } finally {
            if (scaffold != null) {
                scaffold.rollback();
            }
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }


    public boolean validateSitemap(Node sitemap, Route route) throws RepositoryException {
        // e. g. /news/:date/:id or // /text/*path
        String urlMatcher = route.getUrl();
        // e. g. /news/date:String/id:String
        String contentPath = route.getContentPath();
        List<Route.Parameter> parameters = route.getParameters();

        Scanner scanner = new Scanner(urlMatcher);
        scanner.useDelimiter(Pattern.compile("/"));

        Node sitemapItem = sitemap;
        while (scanner.hasNext()) {
            String path = scanner.next();
            if (path.startsWith(":")) {
                sitemapItem = sitemapItem.getNode("_default");
            } else if (path.startsWith("*")) {
                sitemapItem = sitemapItem.getNode("_any");
            } else {
                sitemapItem = sitemapItem.getNode(path);
            }
        }

        // todo replace contentPath placeHolders and check if we see the contentPath

        return true;
    }


//    public void testUpdate() {
//        final Map<String, String> start = TestUtils.dirHash(new File(PROJECT_DIR));
//
//        Scaffold scaffold = Scaffold.instance();
//        scaffold.build();
//
//        final Map<String, String> beforeUpdate = TestUtils.dirHash(new File(PROJECT_DIR));
//
//        assertTrue(new File(new File(PROJECT_DIR), HSTScaffold.SCAFFOLD_DIR_NAME).exists());
//
//        assertTrue(filesBuild);
//
//        scaffold.addRoute(new Route("/update", "update", "update(header,main(banner,text),footer)"));
//
//        scaffold.update();
//
//        assertTrue(filesUpdated);
//
//        scaffold.rollback();
//
//        final Map<String, String> afterUpdate = TestUtils.dirHash(new File(PROJECT_DIR));
//        assertFalse(TestUtils.dirChanged(beforeUpdate, afterUpdate));
//
//        scaffold.rollback();
//
//        final Map<String, String> end = TestUtils.dirHash(new File(PROJECT_DIR));
//        assertFalse(TestUtils.dirChanged(start, end));
//    }

}
