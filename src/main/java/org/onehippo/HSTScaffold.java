package org.onehippo;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class HSTScaffold {

    // TODO add logging

    public static void main( String[] args ) {
        // create the parser
        CommandLineParser parser = new BasicParser();
        try {
            Options options = new Options();
            // todo render help overview
            options.addOption("h", "help", false, "show help.");

            options.addOption("b", "build", true, "Build configuration from scaffold.");
            options.addOption("u", "update", true, "Update configuration from  scaffold.");
            options.addOption("r", "rollback", true, "Rollback configuration changes.");
            options.addOption("c", "configuration", true, "Custom configuration file.");

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // todo invoke scaffold build, update, rollback
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage() );
        }

    }
}
