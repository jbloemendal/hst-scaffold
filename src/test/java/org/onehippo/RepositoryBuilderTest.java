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

        if (!templates.hasNode(component.getTemplateName())) {
            return false;
        }

        Node template = templates.getNode(component.getTemplateName());
        if (!"hst:template".equals(template.getProperty("jcr:primaryType").getString())) {
            return false;
        }

        return true;
    }

    private boolean areTemplateIncludesValid(Node hstSiteCnfRoot, Route.Component component) throws IOException {
        String template = TestUtils.readFile(component.getTemplateFilePath());
        for (Route.Component child : component.getComponents()) {
            if (!template.contains("<@hst.include ref=\""+child.getName()+"\" />")) {
                return false;
            }
        }
        return true;
    }

    private boolean validateComponent(Node templates, Node root, Route.Component component) throws XPathExpressionException, RepositoryException, IOException {
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

        // todo add test for referenced component structures
        if (component.isReference() || component.isPointer()) {
            // skip
            return true;
        }

        if (!root.hasNode(component.getName())) {
            return false;
        }

        Node componentNode = root.getNode(component.getName());
        if (!isHstComponentConfValid(component, componentNode)
                || !isHstTemplateConfValid(templates, component)
                || !areTemplateIncludesValid(root, component)) {
            return false;
        }


        for (Route.Component child : component.getComponents()) {
            if (!componentNode.hasNode(child.getName())) {
                return false;
            }

            return validateComponent(templates, componentNode, child);
        }

        return true;
    }

    private boolean isHstComponentConfValid(Route.Component component, Node componentNode) throws RepositoryException {
        if (componentNode == null) {
            return false;
        }

        if (!componentNode.hasProperty(JCR_PRIMARY_TYPE)
                || !HST_COMPONENT.equals(componentNode.getProperty(JCR_PRIMARY_TYPE).getString())) {
            return false;
        }
        if (!componentNode.hasProperty(HST_COMPONENTCLASSNAME)
                || !component.getJavaClass().equals(componentNode.getProperty(HST_COMPONENTCLASSNAME).getString())) {
            return false;
        }

        File javaFile = new File(component.getPathJavaClass());
        assertTrue(javaFile.exists());

        if (!componentNode.hasProperty(HST_TEMPLATE)
                || !component.getName().equals(componentNode.getProperty(HST_TEMPLATE).getString())) {
            return false;
        }

        return true;
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

        if (!sitemapItem.hasProperty("hst:componentconfigurationid")) {
            return false;
        }
        if (!("hst:pages/"+route.getPage().getName()).equals(sitemapItem.getProperty("hst:componentconfigurationid").getString())) {
            return false;
        }
        if (!sitemapItem.hasProperty("hst:relativecontentpath")) {
            return false;
        }

        int index = 1;
        for (Route.Parameter param : parameters) {
            contentPath = contentPath.replace(param.name+":"+param.type, "${"+index+"}");
            index++;
        }

        if (!contentPath.substring(1).equals(sitemapItem.getProperty("hst:relativecontentpath").getString())) {
            return false;
        }

        return true;
    }


    public void testRoutes() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance("./myhippoproject");

            Node hst = JcrMockUp.mockJcrNode("/hst.xml");

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
            log.error("Error testing components, XPath expression", e);
        } finally {
            if (scaffold != null) {
                scaffold.rollback(false);
            }
        }

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }

}
