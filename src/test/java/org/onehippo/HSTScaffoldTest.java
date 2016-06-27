package org.onehippo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Unit test for HSTScaffold.
 */
public class HSTScaffoldTest extends TestCase {

    final static Logger log = Logger.getLogger(HSTScaffold.class);
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
        assertTrue((projectDir+"/"+HSTScaffold.DEFAULT_TEMPLATE_PATH+"/text/header.ftl").equals(header.getTemplate()));
        assertTrue((projectDir+"/"+HSTScaffold.DEFAULT_COMPONENT_PATH+"/HeaderComponent.java").equals(header.getJavaClass()));
    }

    public void testScaffoldRoutes() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        assertEquals(routes.size(), 5);
    }

    public void testUrlParameter() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(3).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals(type, "String");
    }

    public void testWildcardUrlParameter() {
        HSTScaffold scaffold = HSTScaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(4).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals(type, "String");
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


    private void validateTemplate(Route.Component component) {
//            /*
//            <sv:node sv:name="base-top-menu">
//            <sv:property sv:name="jcr:primaryType" sv:type="Name">
//            <sv:value>hst:template</sv:value>
//            </sv:property>
//            <sv:property sv:name="hst:renderpath" sv:type="String">
//            <sv:value>webfile:/freemarker/gogreen/base-top-menu.ftl</sv:value>
//            </sv:property>
//            </sv:node>
//            */
    }

    private void validateTemplateIncludes(Route.Component component) {
//        Reader reader = new BufferedReader(new FileReader(new File((component.getTemplate()))));
//        Scanner scanner = new Scanner(reader);
//
//        assertTrue(scanner.hasNext(Pattern.compile("<@hst.include ref=\""+component.getName()+"\">")));
//
//        for (Component component : component.getComponents()) {
//            testTemplateInclude(component);
//        }
    }

    private void validateJavaComponent(Route.Component component) {

    }

    private boolean validateComponent(Route.Component component) throws XPathExpressionException {
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
        String javaClass = component.getJavaClass();
        String template = component.getTemplate();

        validateTemplate(component);
        validateTemplateIncludes(component);
        validateJavaComponent(component);

        for (Route.Component child : component.getComponents()) {
            if (!validateComponent(child)) {
                return false;
            }
        }

        return true;
    }


    public void testComponents() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance();

            Node hst = JcrMockUp.mockJcrNode("/hst.xml");
            scaffold.setBuilder(new RepositoryBuilder(hst));

            scaffold.build();

            for (Route route : scaffold.getRoutes()) {
                validateComponent(route.getPage());
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
