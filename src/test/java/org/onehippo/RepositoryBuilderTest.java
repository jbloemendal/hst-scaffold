package org.onehippo;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
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
    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RepositoryBuilderTest(String testName) {
        super(testName);

        HSTScaffold.instance();
        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
    }

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

    private boolean isTemplateIncludesValid(Node hstSiteCnfRoot, Route.Component component) throws IOException {
        String template = TestUtils.readFile(component.getTemplateFilePath());
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

    private boolean validateSitemap(Node sitemap, Route route) throws RepositoryException {
        //        <?xml version="1.0" encoding="UTF-8"?>
        //        <sv:node sv:name="_any_" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
        //        <sv:property sv:name="jcr:primaryType" sv:type="Name">
        //        <sv:value>hst:sitemapitem</sv:value>
        //        </sv:property>
        //        <sv:property sv:name="jcr:uuid" sv:type="String">
        //        <sv:value>5734c9df-401c-4acc-99ef-ee64357fe348</sv:value>
        //        </sv:property>
        //        <sv:property sv:name="hst:componentconfigurationid" sv:type="String">
        //        <sv:value>hst:pages/newslist</sv:value>
        //        </sv:property>
        //        <sv:property sv:name="hst:relativecontentpath" sv:type="String">
        //        <sv:value>${parent}/${1}</sv:value>
        //        </sv:property>
        //        </sv:node>

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
                sitemapItem = sitemapItem.getNode("_default_");
            } else if (path.startsWith("*")) {
                sitemapItem = sitemapItem.getNode("_any_");
            } else {
                sitemapItem = sitemapItem.getNode(path);
            }

            // todo add tests here as well

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
            contentPath.replace(param.name+":"+param.type, "${"+index+"}");
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

    public void testMyAss() {
        boolean big = Boolean.TRUE;
        assertTrue(big);
    }

}
