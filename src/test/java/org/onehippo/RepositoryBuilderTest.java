package org.onehippo;

import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.build.RepositoryBuilder;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;


public class RepositoryBuilderTest extends TestCase {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    public static final String HST_TEMPLATE = "hst:template";
    public static final String HST_CONTAINERITEMCOMPONENT = "hst:containeritemcomponent";
    public static final String HST_COMPONENT = "hst:component";
    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RepositoryBuilderTest(String testName) throws IOException {
        super(testName);

        HSTScaffold.instance("./myhippoproject");
        projectDir = new File("./myhippoproject");
    }

    private boolean isHstTemplateConfValid(Node templates, Route.Component component) throws RepositoryException {
        if (templates.hasNode(component.getTemplateName())) {
            Node template = templates.getNode(component.getTemplateName());
            if ("hst:template".equals(template.getProperty("jcr:primaryType").getString())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean areTemplateIncludesValid(Node templates, Route.Component component) throws IOException, RepositoryException {
        Node templateNode = templates.getNode(component.getName());
        if (templateNode.hasProperty("hst:renderpath")) {
            String renderPath = templateNode.getProperty("hst:renderpath").getString();

            String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
            String basePath = HSTScaffold.properties.getProperty(HSTScaffold.WEBFILE_BASE_PATH);

            renderPath = renderPath.replace(basePath+projectName, "");

            String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
            String templatePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);

            String template = TestUtils.readFile(projectDir+"/"+templatePath+"/"+renderPath);
            for (Route.Component child : component.getComponents()) {
                if (!template.contains("<@hst.include ref=\""+child.getName()+"\" />")) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean validateComponent(Node templates, Node root, Route.Component component) throws XPathExpressionException, RepositoryException, IOException {
        // todo add test for referenced component structures
        if (component.isReference() && component.isPointer()) {
            if (root.hasNode(component.getName())) {
                Node componentNode = root.getNode(component.getName());
                if (isHstComponentConfValid(component, componentNode)
                        && isHstTemplateConfValid(templates, component)
                        && areTemplateIncludesValid(templates, component)) {
                    for (Route.Component child : component.getComponents()) {
                        if (componentNode.hasNode(child.getName())) {
                            return validateComponent(templates, componentNode, child);
                        } else {
                            return false;
                        }
                    }
                } else {
                    return false;
                }

                return true;
            } else {
                return false;
            }
        } else {
            // skip
            return true;
        }
    }

    private boolean isHstComponentConfValid(Route.Component component, Node componentNode) throws RepositoryException {
        if (componentNode == null) {
            return false;
        }

        if (componentNode.hasProperty(JCR_PRIMARY_TYPE)
                && HST_COMPONENT.equals(componentNode.getProperty(JCR_PRIMARY_TYPE).getString())) {
            if (componentNode.hasProperty(HST_COMPONENTCLASSNAME)
                    && component.getJavaClass().equals(componentNode.getProperty(HST_COMPONENTCLASSNAME).getString())) {
                File javaFile = new File(component.getPathJavaClass());
                assertTrue(javaFile.exists());

                if (componentNode.hasProperty(HST_TEMPLATE)
                        && component.getName().equals(componentNode.getProperty(HST_TEMPLATE).getString())) {
                } else {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    private boolean validateSitemap(Node sitemap, Route route) throws RepositoryException {
        // e. g. /news/:date/:id or // /text/*path
        String urlMatcher = route.getUrl();
        // e. g. /news/date:String/id:String
        String contentPath = route.getContentPath();
        List<Route.Parameter> parameters = route.getParameters();

        Scanner scanner = new Scanner(urlMatcher);
        scanner.useDelimiter(Pattern.compile("/"));

        Node sitemapItem = sitemap;
        if ("/".equals(StringUtils.trim(urlMatcher))) {
            sitemapItem = sitemapItem.getNode("root");
        } else {
            while (scanner.hasNext()) {
                String path = scanner.next();
                if (path.startsWith(":")) {
                    sitemapItem = sitemapItem.getNode("_default_");
                } else if (path.startsWith("*")) {
                    sitemapItem = sitemapItem.getNode("_any_");
                } else {
                    sitemapItem = sitemapItem.getNode(path);
                }
                // todo add tests here as well
            }
        }

        if (sitemapItem.hasProperty("hst:componentconfigurationid")) {
            if (("hst:pages/"+route.getPage().getName()).equals(sitemapItem.getProperty("hst:componentconfigurationid").getString())) {
                if (sitemapItem.hasProperty("hst:relativecontentpath")) {
                    int index = 1;
                    for (Route.Parameter param : parameters) {
                        contentPath = contentPath.replace(param.name+":"+param.type, "${"+index+"}");
                        index++;
                    }

                    if (contentPath.substring(1).equals(sitemapItem.getProperty("hst:relativecontentpath").getString())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    public void testRoutes() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance("./myhippoproject");

            Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");

            scaffold.setBuilder(new RepositoryBuilder(hst));
            scaffold.build(false);

            String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

            Node pages = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:pages");
            Node sitemap = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:sitemap");
            Node templates = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:templates");

            for (Route route : scaffold.getRoutes()) {
                assertTrue(String.format("assert valid sitemap for route %s", route.getUrl()), validateSitemap(sitemap, route));
                assertTrue(String.format("assert valid component for route %s -> %s", route.getUrl(), route.getPageConstruct()), validateComponent(templates, pages, route.getPage()));
            }

        } catch (Exception e) {
            log.error("Error testing components.", e);
        } finally {
            if (scaffold != null) {
                scaffold.rollback(false);
            }
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }


    public void testPageInheritance() throws IOException {
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        List<Route> routes = scaffold.getRoutes();

        // reference
        assertTrue(routes.get(1).getPage().getName().equals("text"));
        assertTrue(routes.get(1).getPage().getComponents().get(1).getName().equals("main"));
        assertTrue(routes.get(1).getPage().getComponents().get(1).isPointer());
        assertTrue(routes.get(1).getPage().isReference());

        // pointer
        assertTrue(routes.get(5).getPage().getName().equals("text"));
        assertTrue(routes.get(5).getPage().isPointer());

        final Map<String, String> before = TestUtils.dirHash(projectDir);

        try {
            Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");
            scaffold.setBuilder(new RepositoryBuilder(hst));
            scaffold.build(false);
        } catch (Exception e) {
            log.error("Error testing components", e);
        } finally {
            scaffold.rollback(false);
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }


    public void testMainMenu() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance("./myhippoproject");

            Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");

            scaffold.setBuilder(new RepositoryBuilder(hst));
            scaffold.build(false);

            String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

            assertTrue(hst.getNode("hst:configurations").getNode(projectHstNodeName).hasNode("hst:sitemenus"));

            Node menus = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:sitemenus");

            assertTrue(menus.hasNode("main"));

            Node main = menus.getNode("main");

            for (Route route : scaffold.getRoutes()) {
                if (route.getParameters().size() > 0) {
                    continue;
                }

                String[] names = route.getUrl().split("/");
                if (names.length == 0) {
                    continue;
                }
                String name = names[names.length - 1];
                if (StringUtils.isEmpty(name)) {
                    continue;
                }

                assertTrue(main.hasNode(name));
                Node menuItem = main.getNode("main");

                String sitemapPath = route.getUrl().substring(1);

                String reference = menuItem.getProperty("hst:referencesitemapitem").getString();
                if (StringUtils.isNotEmpty(reference)) {
                    assertEquals(sitemapPath, reference);
                }
            }

        } catch (Exception e) {
            log.error("Error testing components.", e);
        } finally {
            if (scaffold != null) {
                scaffold.rollback(false);
            }
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }

    public void testSpecificMenuItems() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance("./myhippoproject");

            Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");

            scaffold.setBuilder(new RepositoryBuilder(hst));
            scaffold.build(false);

            String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

            assertTrue(hst.getNode("hst:configurations").getNode(projectHstNodeName).hasNode("hst:sitemenus"));

            Node menus = hst.getNode("hst:configurations").getNode(projectHstNodeName).getNode("hst:sitemenus");

            assertTrue(menus.hasNode("main"));

            Node main = menus.getNode("main");

            assertTrue(main.hasNode("home"));
            assertTrue(main.hasNode("contact"));
            assertTrue(main.hasNode("simple"));
            assertTrue(main.hasNode("news"));

        } catch (Exception e) {
            log.error("Error testing components.", e);
        } finally {
            if (scaffold != null) {
                scaffold.rollback(false);
            }
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }

}
