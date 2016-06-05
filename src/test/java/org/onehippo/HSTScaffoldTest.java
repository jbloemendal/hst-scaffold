package org.onehippo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Unit test for simple HSTScaffold.
 */
// TODO pseudo codes
public class HSTScaffoldTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HSTScaffoldTest(String testName) {
        super(testName);
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

    // Test Parsing
    public void testScaffoldRoutes() {
        Scaffold scaffold = Scaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        assertEquals(routes.length(), 5);
    }

    public void testUrlParameter() {
        Scaffold scaffold = Scaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        Map<String,String> urlParameters = routes.get(3).getUrlMatcher().getParameters();
        String type = urlParameters.get("id");
        assertEquals(type, "String");
    }

    public void testWildcardUrlParameter() {
        Scaffold scaffold = Scaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        Map<String,String> urlParameters = routes.get(4).getUrlMatcher().getParameters();
        String type = urlParameters.get("path");
        assertEquals(type, "String");
    }

    public void testPages() {
        Scaffold scaffold = Scaffold.instance();
        List<Route> routes = scaffold.getRoutes();
        Page page = routes.get(0).getPage();
        List<Component> components = page.getComponents();
        // TODO
        // component.getTemplate();
        // component.getJavaClass();

        assertEquals(components.length(), 3);

        Component main = components.get(1);
        assertEquals(main.getName(), "main");

        components = main.getComponents();

        assertEquals(components.length, 2);
    }

    // TODO test actual scaffolding, xml, ftl, files
    public void testComponents() {
        Scaffold scaffold = Scaffold.instance();

        try {
            // todo create a backup of directories which are going to be changed
            // todo provide a dryrun option
            scaffold.build();

            File componentsFile = new File("components.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(componentsFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            assertTrue(componentsCreated);
        } catch (SAXException e) {
            e.printStackTrace(); // TODO
        } catch (ParserConfigurationException e) {
            e.printStackTrace(); // TODO
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        } finally {
            scaffold.rollback();
        }

    }

    public void testTemplates() {
        Scaffold scaffold = Scaffold.instance();

        try {
            scaffold.build();

            File componentsFile = new File("components.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(componentsFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            assertTrue(xmlValidates);

            assertTrue(elementsPresent);

            assertTrue(templatesFtlCreated);

        } catch(Exception e) {
            scaffold.rollback();
        }
    }

    public void testTemplateIncludes() {
        Scaffold scaffold = Scaffold.instance();

        try {
            scaffold.build();

            File componentsFile = new File("components.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(componentsFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            // home(header,main(banner, text),footer)
            assertTrue(templateIncludesExist);
        } catch(Exception e) {
            scaffold.rollback();
        }
    }

    public void testDryRun() {
        Scaffold scaffold = Scaffold.instance();

        // persist data to dry run .scaffold/dryrun directory instead of project
        scaffold.dryRun();

        assertTrue(filesDeleted);
    }

    public void testRollback() {
        Scaffold scaffold = Scaffold.instance();

        scaffold.build();

        assertTrue(filesBuild);

        scaffold.rollback();

        assertTrue(projectAtInitialState);
    }

    public void testUpdateDryRun() {
        Scaffold scaffold = Scaffold.instance();

        scaffold.build();

        assertTrue(filesBuild);

        scaffold.addRoute(new Route("/dryrun", "dryrun", "dryrun(header,main(banner,text),footer)"));

        scaffold.dryRun();

        assertTrue(dryRunUpdated);

        scaffold.rollback();

        assertTrue(filesDeleted);
    }

    public void testUpdate() {
        Scaffold scaffold = Scaffold.instance();

        scaffold.build();

        assertTrue(filesBuild);

        scaffold.update();

        assertTrue(filesUpdated);

        scaffold.rollback();

        assertTrue(filesDeleted);
    }

}
