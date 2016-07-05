package org.onehippo;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

import javax.jcr.*;
import java.io.IOException;

public class HSTScaffoldCLI {

    final static Logger log = Logger.getLogger(HSTScaffoldCLI.class);

    public static void main( String[] args ) {
        // create the parser
        CommandLineParser parser = new BasicParser();
        try {
            Options options = new Options();

            options.addOption("h", "help", false, "show help.");
            options.addOption("b", "build", false, "Build configuration from scaffold.");
            options.addOption("c", "configuration file", true, "Custom configuration file.");
            options.addOption("u", "update", false, "Update configuration from scaffold.");
            options.addOption("s", "Build scaffold from existing project configuration.", true, "Build scaffold from configuration (reverse)");
            options.addOption("r", "rollback", true, "Rollback configuration changes.");

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("h")) {
                log.info(options.toString());
            } else if (line.hasOption("b")) {
                HSTScaffold scaffold = HSTScaffold.instance(".");

                String uri = HSTScaffold.properties.getProperty("hippo.rmi.uri");
                String user = HSTScaffold.properties.getProperty("hippo.rmi.user");
                String password = HSTScaffold.properties.getProperty("hippo.rmi.password");

                log.info(String.format("Connecting to %s", uri));

                HippoRepository repo = HippoRepositoryFactory.getHippoRepository(uri);
                Session session = repo.login(user, password.toCharArray());
                Node hst = session.getNode("/hst:hst");

                scaffold.setBuilder(new RepositoryBuilder(hst));
                try {
                    scaffold.build(false);
                    session.save();
                } catch (Exception e) {
                    scaffold.rollback(false);
                    session.refresh(false);
                    throw e;
                }

            }
            // todo ask if there are several hst confs, which we should choose

            // todo invoke scaffold build, update, rollback

            // todo print changed / created files (^M/^C)
        } catch(ParseException exp ) {
            log.error("Parsing failed.", exp);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage() );
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (NoSuchWorkspaceException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
