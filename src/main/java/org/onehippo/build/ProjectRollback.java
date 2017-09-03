package org.onehippo.build;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.Arrays;

public class ProjectRollback implements Rollback {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private Node hstRoot;
    private Node projectHstConfRoot;
    private File projectDir;
    private File scaffoldDir;

    public ProjectRollback(Node hstRoot) throws RepositoryException, IOException {
        this.hstRoot = hstRoot;

        String projectHstNodeName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        projectHstConfRoot = hstRoot.getNode("hst:configurations").getNode(projectHstNodeName);

        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
        if (!projectDir.exists()) {
            throw new IOException(String.format("Project directory doesn't exist %s.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }

        scaffoldDir = new File(projectDir, ".scaffold");
        if (!scaffoldDir.exists()) {
            scaffoldDir.mkdirs();
        }
    }

    public void backup(boolean dryRun) throws IOException, RepositoryException {
        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);

        File backup = new File(scaffoldDir, "history/"+System.currentTimeMillis());
        log.info(String.format("%s Creating backup directory %s", (dryRun? "DRYRUN " : ""), backup.getPath()));
        if (!dryRun) {
            backup.mkdirs();
        }

        File hstConfFile = new File(backup, projectName+"_hst.xml");
        log.info(String.format("%s Export hst \"%s\" config %s", (dryRun? "DRYRUN " : ""), projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new FileOutputStream(hstConfFile);
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }

        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);
        log.info(String.format("%s Backup java components %s and templates %s", (dryRun? "DRYRUN " : ""), componentDirectory.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(componentDirectory, new File(backup, "java"));
            FileUtils.copyDirectory(templateDirectory, new File(backup, "ftl"));
        }
    }

    private File getLatestBackup() {
        // find latest backup folder
        File backups = new File(scaffoldDir, "history");

        File[] files = backups.listFiles();
        Arrays.sort(files);
        if (files.length == 0) {
            log.info("No backups available.");
            return null;
        }

        return files[files.length-1];
    }

    public void rollback(boolean dryRun) throws IOException, RepositoryException {
        log.info(String.format("%s Move current project conf into trash.", (dryRun? "DRYRUN " : "")));

        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);
        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);
        File drafts = new File(scaffoldDir, "drafts/"+System.currentTimeMillis());
        if (!drafts.exists()) {
            drafts.mkdirs();
        }
        File hstConfFile = new File(drafts, projectName+"_hst.xml");

        saveDraft(dryRun, projectName, componentDirectory, templateDirectory, drafts, hstConfFile);

        log.info("Restore backup");
        File latest = getLatestBackup();

        restoreFromBackup(dryRun, componentDirectory, templateDirectory, latest);
        restoreHstConfig(dryRun, projectName, latest);
        removeBackup(dryRun, latest);
    }

    private void removeBackup(boolean dryRun, File latest) throws IOException {
        // remove backup folder
        log.info(String.format("%s Delete backup %s", (dryRun? "DRYRUN " : ""), latest));
        if (!dryRun) {
            FileUtils.deleteDirectory(latest);
        }
    }

    private void restoreHstConfig(boolean dryRun, String projectName, File latest) throws RepositoryException, IOException {
        File hstConfFile;// restore hst config
        hstConfFile = new File(latest, projectName+"_hst.xml");
        log.info(String.format("%s Import hst \"%s\" config %s", (dryRun? "DRYRUN " : ""), projectName, hstConfFile.getPath()));
        if (!dryRun) {
            projectHstConfRoot.getSession().removeItem(projectHstConfRoot.getPath());

            Node hstConfigurations = hstRoot.getNode("hst:configurations");
            InputStream in = new BufferedInputStream(new FileInputStream(hstConfFile));
            projectHstConfRoot.getSession().importXML(hstConfigurations.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }
    }

    private void restoreFromBackup(boolean dryRun, File componentDirectory, File templateDirectory, File latest) throws IOException {
        // copy java and templates from backup folder
        File latestJavaFilesBackup = new File(latest, "java");
        File latestTemplateFilesBackup = new File(latest, "ftl");

        log.info(String.format("%s Copy backup: java components %s into %s", (dryRun? "DRYRUN " : ""), latestJavaFilesBackup.getPath(),  componentDirectory.getPath()));
        log.info(String.format("%s Copy backup: templates %s into %s", (dryRun? "DRYRUN " : ""), latestTemplateFilesBackup.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(latestJavaFilesBackup, componentDirectory);
            FileUtils.copyDirectory(latestTemplateFilesBackup, templateDirectory);
        }
    }

    private void saveDraft(boolean dryRun, String projectName, File componentDirectory, File templateDirectory, File drafts, File hstConfFile) throws IOException, RepositoryException {
        log.info(String.format("%s Move java components %s and templates %s into %s", (dryRun? "DRYRUN " : ""), componentDirectory.getPath(), templateDirectory.getPath(), drafts.getPath()));
        // move current projects java and templates into .scaffold/drafts/date
        if (!dryRun) {
            FileUtils.moveDirectory(componentDirectory, new File(drafts, "java"));
            FileUtils.moveDirectory(templateDirectory, new File(drafts, "ftl"));
        }

        // export current projects hst conf to .scaffold/drafts/date
        log.info(String.format("%s Export hst \"%s\" config %s", (dryRun? "DRYRUN " : ""), projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }
    }

}
