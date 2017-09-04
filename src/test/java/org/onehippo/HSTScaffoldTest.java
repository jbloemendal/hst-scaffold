package org.onehippo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.onehippo.examination.Diagnose;

import java.io.*;
import java.util.List;
import java.util.Map;

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
    public HSTScaffoldTest(String testName) throws IOException {
        super(testName);

        HSTScaffold.instance("./myhippoproject");
        projectDir = new File("./myhippoproject");
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
        String componentPath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        //todo assertTrue((projectDir+"/"+HSTScaffold.DEFAULT_TEMPLATE_PATH+"/text/header.ftl").equals(header.getTemplateFilePath()));
        assertTrue((projectDir+"/"+componentPath+"/HeaderComponent.java").equals(header.getPathJavaClass()));
    }

    public void testScaffoldRoutes() throws IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        List<Route> routes = scaffold.getRoutes();
        assertEquals(7, routes.size());
    }

    public void testUrlParameter() throws IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(3).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals("String", type);
    }

    public void testWildcardUrlParameter() throws IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        List<Route> routes = scaffold.getRoutes();
        List<Route.Parameter> urlParameters = routes.get(5).getParameters();
        String type = urlParameters.get(0).type;
        assertEquals("String", type);
    }

    public void testPointerReference() throws IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        List<Route> routes = scaffold.getRoutes();
        assertTrue(routes.get(0).getPage().getComponents().get(1).getName().equals("main"));
        assertTrue(routes.get(0).getPage().getComponents().get(1).isReference());
        assertTrue(routes.get(3).getPage().getComponents().get(1).isPointer());
    }

    public void testDiagnoseSitemap() throws IOException {
        // TODO
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        Map<String, Diagnose> result = scaffold.examine();
    }

    public void testDiagnoseMenu() throws IOException {
        // TODO
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        Map<String, Diagnose> result = scaffold.examine();
    }

    public void testDiagnoseComponents() throws IOException {
        // TODO
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        Map<String, Diagnose> result = scaffold.examine();
    }

    public void testDiagnoseTemplates() throws IOException {
        // TODO
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
        Map<String, Diagnose> result = scaffold.examine();
    }

}
