package org.onehippo.build;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onehippo.HSTScaffold;
import org.onehippo.Route;

import javax.jcr.RepositoryException;
import java.io.*;
import java.util.HashMap;

public class TemplateBuilder {

    final static Logger log = Logger.getLogger(RepositoryBuilder.class);

    private File projectDir;
    private File scaffoldDir;

    public TemplateBuilder() throws RepositoryException, IOException {
        projectDir = new File(HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR));
        if (!projectDir.exists()) {
            throw new IOException(String.format("Project directory doesn't exist %s.", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR)));
        }

        scaffoldDir = new File(projectDir, ".scaffold");
        if (!scaffoldDir.exists()) {
            scaffoldDir.mkdirs();
        }
    }

    public void buildComponentJavaFile(final Route.Component component, boolean dryRun) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();

        String templatePath = "";
        File template = new File(scaffoldDir, "Component.java.mustache");
        if (template.exists()) {
            templatePath = template.getPath();
        } else {
            templatePath = this.getClass().getResource("/Component.java.mustache").getFile();
        }

        Mustache mustache = mf.compile(templatePath);

        Writer writer= new PrintWriter(System.out);;
        File javaClassFile = new File(component.getPathJavaClass());
        javaClassFile.getParentFile().mkdirs();

        log.info(String.format("%s Build component %s java class %s", (dryRun? "DRYRUN " : ""), component.getName(), javaClassFile.getPath()));

        if (!dryRun) {
            if (javaClassFile.exists()) {
                log.info(String.format("Java component file %s alreday exists", javaClassFile.getPath()));
            } else {
                writer = new FileWriter(javaClassFile);
            }
        }

        try {
            mustache.execute(writer, new HashMap<String, String>() {
                {
                    put("projectPackage", HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_PACKAGE_NAME));
                    put("name", StringUtils.capitalize(component.getName().toLowerCase()));
                }
            }).flush();
        } finally {
            writer.close();
        }
    }

    public void buildTemplateFtlFile(final Route.Component component, boolean dryRun) throws IOException, RepositoryException {
        MustacheFactory mf = new DefaultMustacheFactory();

        String templatePath = "";
        File template = new File(scaffoldDir, "template.ftl.mustache");
        if (template.exists()) {
            templatePath = template.getPath();
        } else {
            templatePath = this.getClass().getResource("/template.ftl.mustache").getFile();
        }

        Mustache mustache = mf.compile(templatePath);

        Writer writer = new PrintWriter(System.out);

        File templateFile = new File(component.getTemplateFilePath());

        templateFile.getParentFile().mkdirs();

        log.info(String.format("%s Build %s component template %s", (dryRun? "DRYRUN " : ""), component.getName(), templateFile.getPath()));

        if (!dryRun) {
            if (templateFile.exists()) {
                log.info(String.format("Template file %s already exists.", templateFile.getPath()));
            } else {
                writer = new FileWriter(templateFile);
            }
        }

        try {
            mustache.execute(writer, new HashMap<String, Object>() {
                {
                    put("childs", component.getComponents());
                    put("name", component.getName().toLowerCase());
                }
            }).flush();
        } finally {
            writer.close();
        }

    }

}
