package org.onehippo;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;

public class HSTScaffoldCLITest extends TestCase {

    final static Logger log = Logger.getLogger(HSTScaffoldCLITest.class);

    private File projectDir;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HSTScaffoldCLITest(String testName) {
        super(testName);
        HSTScaffold.instance();
        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
    }

    public void testHuman() {
        int five = 4;
        assertTrue(five % 2 == 0);
    }

    public void testDryRun() {
        final Map<String, String> before = TestUtils.dirHash(projectDir);

        HSTScaffold scaffold = HSTScaffold.instance();

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
