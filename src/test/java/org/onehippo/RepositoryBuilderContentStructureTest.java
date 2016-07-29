package org.onehippo;


import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.onehippo.build.RepositoryBuilder;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;

import javax.jcr.Node;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepositoryBuilderContentStructureTest extends TestCase {

    private File projectDir;
    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    public Pattern PATH_SEGMENT = Pattern.compile("/[^/]+/");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RepositoryBuilderContentStructureTest(String testName) throws IOException {
        super(testName);

        HSTScaffold.instance("./myhippoproject");
        projectDir = new File("./myhippoproject");
    }

    /*
    #URL              CONTENTPATH                   COMPONENTS
    /                 /home                         home(header,&main(banner, doc),footer) # home page
    /contact          /contact                      text(header,*main,footer)              # text page
    /news/:date/:id   /news/date:String/id:String   news(header,*main,footer)              # news page
    /news             /news                         newsoverview(header,list, footer)      # news overview page
    /text/*path       /text/path:String             text(header,*main,footer)
    */
    public void testContentStructure() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = null;
        try {
            scaffold = HSTScaffold.instance("./myhippoproject");

            Node root = JcrMockUp.mockJcrNode("/cafebabe.xml");

            scaffold.setBuilder(new RepositoryBuilder(root.getNode("hst:hst")));
            scaffold.build(false);

            Node documents = root.getNode("content").getNode("documents").getNode(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME));
            for (Route route : scaffold.getRoutes()) {
                Matcher matcher = PATH_SEGMENT.matcher(route.getContentPath());
                Node folderRoot = documents;
                while (matcher.find()) {
                    String folderName = matcher.group().replaceAll("/", "");
                    if (folderName.contains(":")) {
                        break;
                    }
                    assertTrue(folderRoot.hasNode(folderName));
                    folderRoot = folderRoot.getNode(folderName);
                    folderRoot.hasProperty("hippostd:foldertype");
                }
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
