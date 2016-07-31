package org.onehippo;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.onehippo.build.RepositoryBuilder;

import javax.jcr.*;
import java.io.IOException;

public class HSTScaffoldCLI {

    final static Logger log = Logger.getLogger(HSTScaffoldCLI.class);

    public static void main( String[] args ) {
        CommandLineParser parser = new BasicParser();
        try {
            Options options = getOptions();
            CommandLine line = parser.parse(options, args);

            boolean dryRun = false;
            if (line.hasOption("d")) {
                dryRun = true;
            }

            HSTScaffold scaffold = HSTScaffold.instance(".");

            if (line.hasOption("h")) {
                printHelp(options);
            } else if (line.hasOption("b")) {
                build(scaffold, dryRun);
            } else if (line.hasOption("r")) {
                rollback(scaffold, dryRun);
            } else {
                printHelp(options);
            }
        } catch(ParseException exp ) {
            log.error("Parsing failed.", exp);
        } catch (LoginException e) {
            log.error("Repository login failed.", e);
        } catch (RepositoryException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }

    }

    private static void printHelp(Options options) {
        for (Object obj : options.getOptions()) {
            if (obj instanceof Option) {
                Option option = (Option)obj;
                System.out.println(String.format("-%s\t--%s,\t%s", option.getOpt(), option.getLongOpt(), option.getDescription()));
            }
        }
    }


    private static void rollback(HSTScaffold scaffold, boolean dryRun) throws Exception {
        Session session = getSession();

        Node hst = session.getNode("/hst:hst");
        scaffold.setBuilder(new RepositoryBuilder(hst));
        try {
            scaffold.rollback(dryRun);
            session.save();
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
    }

    private static void build(HSTScaffold scaffold, boolean dryRun) throws Exception {
        Session session = getSession();

        Node hst = session.getNode("/hst:hst");
        scaffold.setBuilder(new RepositoryBuilder(hst));
        try {
            scaffold.build(dryRun);
            session.save();
        } catch (Exception e) {
            scaffold.rollback(dryRun);
            session.refresh(false);
            throw e;
        }
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "show help.");
        options.addOption("b", "build", false, "Build configuration from scaffold.");
        options.addOption("d", "dryrun", false, "dryrun, do nothing");
        // options.addOption("f", "file", true, "Custom configuration file.");
        // options.addOption("u", "update", false, "Update configuration from scaffold.");
        // options.addOption("s", "scaffold", true, "Build scaffold from configuration (reverse)");
        options.addOption("r", "rollback", false, "Rollback configuration changes.");
        return options;
    }

    private static Session getSession() throws RepositoryException {
        String uri = HSTScaffold.properties.getProperty("hippo.rmi.uri");
        String user = HSTScaffold.properties.getProperty("hippo.rmi.user");
        String password = HSTScaffold.properties.getProperty("hippo.rmi.password");

        log.info(String.format("Connecting to %s", uri));
        HippoRepository repo = HippoRepositoryFactory.getHippoRepository(uri);
        return repo.login(user, password.toCharArray());
    }
}
