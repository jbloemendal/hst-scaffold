package org.onehippo;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

public class HSTScaffoldCLI {

    final static Logger log = Logger.getLogger(HSTScaffoldCLI.class);

    public static void main( String[] args ) {
        // create the parser
        CommandLineParser parser = new BasicParser();
        try {
            Options options = new Options();

            options.addOption("h", "help", false, "show help.");
            options.addOption("b", "build", true, "Build configuration from scaffold.");
            options.addOption("c", "configuration file", true, "Custom configuration file.");
            options.addOption("u", "update", true, "Update configuration from scaffold.");
            options.addOption("s", "Build scaffold from existing project configuration.", true, "Build scaffold from configuration (reverse)");
            options.addOption("r", "rollback", true, "Rollback configuration changes.");

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // todo invoke scaffold build, update, rollback

            // todo print changed / created files (^M/^C)
        } catch( ParseException exp ) {
            log.error("Parsing failed.", exp);
            System.err.println("Parsing failed.  Reason: " + exp.getMessage() );
        }

    }
}
