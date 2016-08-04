package org.onehippo;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.build.RepositoryBuilder;
import org.onehippo.forge.utilities.commons.jcrmockup.JcrMockUp;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.*;
import java.util.Map;

public class HSTScaffoldCLITest extends TestCase {

    final static Logger log = Logger.getLogger(HSTScaffoldCLITest.class);

    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HSTScaffoldCLITest(String testName) throws IOException, RepositoryException {
        super(testName);
        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");
        scaffold.setBuilder(new RepositoryBuilder(hst));

        projectDir = new File("./myhippoproject");
    }

    public void testHuman() {
        int five = 4;
        int even = 2;
        assertTrue(five % even == 0);
    }

    public void testDryRun() throws IOException, RepositoryException {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");


        Node hst = JcrMockUp.mockJcrNode("/cafebabe.xml").getNode("hst:hst");
        scaffold.setBuilder(new RepositoryBuilder(hst));

        scaffold.build(true);

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }


    public void testRollback() throws IOException {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");

        File history = new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME +"/history");

        scaffold.build(false);

        log.debug("Scaffold history exists? "+history.getPath());
        assertTrue(history.exists());

        scaffold.rollback(false);

        assertTrue(new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME+"/trash").exists());

        final Map<String, String> after = TestUtils.dirHash(projectDir);
        assertFalse(TestUtils.dirChanged(before, after));
    }

//    @Test
//    @Ignore("requires running repository for xml exports")
//    public void testDryRunLocalRepo() throws IOException, RepositoryException {
//        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
//
//        String uri = HSTScaffold.properties.getProperty("hippo.rmi.uri");
//        String user = HSTScaffold.properties.getProperty("hippo.rmi.user");
//        String password = HSTScaffold.properties.getProperty("hippo.rmi.password");
//
//        log.info(String.format("Connecting to %s", uri));
//        HippoRepository repo = HippoRepositoryFactory.getHippoRepository(uri);
//        Session session = repo.login(user, password.toCharArray());
//        Node hst = session.getNode("/hst:hst");
//
//        File hstConfFile = new File(projectDir, "test_rollback_hst.xml");
//        OutputStream out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
//        hst.getSession().exportDocumentView(hst.getPath(), out, true, false);
//
//        final Map<String, String> before = TestUtils.dirHash(projectDir);
//
//        scaffold.setBuilder(new RepositoryBuilder(hst));
//        scaffold.build(true);
//
//        hstConfFile = new File(projectDir, "test_rollback_hst.xml");
//        out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
//        hst.getSession().exportDocumentView(hst.getPath(), out, true, false);
//
//        final Map<String, String> after = TestUtils.dirHash(projectDir);
//        assertFalse(TestUtils.dirChanged(before, after));
//    }

//    @Test
//    @Ignore("requires running repository for xml exports")
//    public void testRollbackLocalRepo() throws IOException, RepositoryException {
//        HSTScaffold scaffold = HSTScaffold.instance("./myhippoproject");
//
//        File history = new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME +"/history");
//
//        String uri = HSTScaffold.properties.getProperty("hippo.rmi.uri");
//        String user = HSTScaffold.properties.getProperty("hippo.rmi.user");
//        String password = HSTScaffold.properties.getProperty("hippo.rmi.password");
//
//        log.info(String.format("Connecting to %s", uri));
//        HippoRepository repo = HippoRepositoryFactory.getHippoRepository(uri);
//        Session session = repo.login(user, password.toCharArray());
//        Node hst = session.getNode("/hst:hst");
//
//        scaffold.setBuilder(new RepositoryBuilder(hst));
//
//        File hstConfFile = new File(projectDir, "test_rollback_hst.xml");
//        try {
//            exportHstConf(hst, hstConfFile);
//
//            final Map<String, String> before = TestUtils.dirHash(projectDir);
//
//            scaffold.build(false);
//
//            log.debug("Scaffold history exists? " + history.getPath());
//            assertTrue(history.exists());
//
//            scaffold.rollback(false);
//
//            exportHstConf(hst, hstConfFile);
//
//            assertTrue(new File(projectDir, HSTScaffold.SCAFFOLD_DIR_NAME + "/trash").exists());
//
//            final Map<String, String> after = TestUtils.dirHash(projectDir);
//            assertFalse(TestUtils.dirChanged(before, after));
//        } catch (Exception e) {
//            log.error(e);
//        } finally {
//             hstConfFile.delete();
//        }
//    }

    private void exportHstConf(Node hst, File hstConfFile) throws IOException, RepositoryException, DocumentException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
        hst.getSession().exportDocumentView(hst.getPath(), out, true, false);

        String hstConfString = FileUtils.readFileToString(hstConfFile);
        hstConfString = hstConfString.replaceAll("jcr:uuid=\"[^\"]+\"", "");
        hstConfString = hstConfString.replaceAll("jcr:versionHistory=\"[^\"]+\"", "");
        hstConfString = hstConfString.replaceAll("jcr:baseVersion=\"[^\"]+\"", "");

        Document doc = DocumentHelper.parseText(hstConfString);
        StringWriter sw = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter xw = new XMLWriter(sw, format);
        xw.write(doc);
        String result = sw.toString();

        FileUtils.writeStringToFile(hstConfFile, result);
    }


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
