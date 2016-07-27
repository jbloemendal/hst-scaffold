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
        File backup = new File(scaffoldDir, "history/"+System.currentTimeMillis());
        log.info(String.format("%s Creating backup directory %s", (dryRun? "DRYRUN " : ""), backup.getPath()));
        if (!dryRun) {
            backup.mkdirs();
        }

        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

        File hstConfFile = new File(backup, projectName+"_hst.xml");
        log.info(String.format("%s Export hst \"%s\" config %s", (dryRun? "DRYRUN " : ""), projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new FileOutputStream(hstConfFile);
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }

        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);

        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);
        log.info(String.format("%s Backup java components %s and templates %s", (dryRun? "DRYRUN " : ""), componentDirectory.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(componentDirectory, new File(backup, "java"));
            FileUtils.copyDirectory(templateDirectory, new File(backup, "ftl"));
        }
    }

    public void rollback(boolean dryRun) throws IOException, RepositoryException {
        log.info("Move current project conf into trash.");
        File trash = new File(scaffoldDir, "trash/"+System.currentTimeMillis());
        if (!trash.exists()) {
            trash.mkdirs();
        }

        // move current projects java and templates into .scaffold/.trash/date
        String javaFilePath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
        String ftlFilePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);
        File componentDirectory = new File(projectDir, javaFilePath);
        File templateDirectory = new File(projectDir, ftlFilePath);

        log.info(String.format("Move java components %s and templates %s into %s", componentDirectory.getPath(), templateDirectory.getPath(), trash.getPath()));
        if (!dryRun) {
            FileUtils.moveDirectory(componentDirectory, new File(trash, "java"));
            FileUtils.moveDirectory(templateDirectory, new File(trash, "ftl"));
        }

        // export current projects hst conf to .scaffold/.trash/date
        String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);

        File hstConfFile = new File(trash, projectName+"_hst.xml");
        log.info(String.format("Export hst \"%s\" config %s", projectName, hstConfFile.getPath()));
        if (!dryRun) {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(hstConfFile));
            projectHstConfRoot.getSession().exportDocumentView(projectHstConfRoot.getPath(), out, true, false);
        }

        log.info("Restore backup");

        // find latest backup folder
        File backups = new File(scaffoldDir, "history");

        File[] files = backups.listFiles();
        Arrays.sort(files);

        File latest = files[files.length-1];

        // copy java and templates from backup folder
        File latestJavaFilesBackup = new File(latest, "java");
        File latestTemplateFilesBackup = new File(latest, "ftl");

        log.info(String.format("Copy backup: java components %s into %s", latestJavaFilesBackup.getPath(),  componentDirectory.getPath()));
        log.info(String.format("Copy backup: templates %s into %s", latestTemplateFilesBackup.getPath(), templateDirectory.getPath()));
        if (!dryRun) {
            FileUtils.copyDirectory(latestJavaFilesBackup, componentDirectory);
            FileUtils.copyDirectory(latestTemplateFilesBackup, templateDirectory);
        }

        // restore hst config
        hstConfFile = new File(latest, projectName+"_hst.xml");
        log.info(String.format("Import hst \"%s\" config %s", projectName, hstConfFile.getPath()));
        if (!dryRun) {
            projectHstConfRoot.getSession().removeItem(projectHstConfRoot.getPath());

            Node hstConfigurations = hstRoot.getNode("hst:configurations");
            InputStream in = new BufferedInputStream(new FileInputStream(hstConfFile));
            projectHstConfRoot.getSession().importXML(hstConfigurations.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }

        // remove backup folder
        log.info(String.format("Delete backup %s", latest));
        if (!dryRun) {
            FileUtils.deleteDirectory(latest);
        }
    }

}
