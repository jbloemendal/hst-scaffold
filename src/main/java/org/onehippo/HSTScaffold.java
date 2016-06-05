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
            options.addOption("h", "help", false, "show help.");
            options.addOption("v", "var", true, "Here you can set parameter.");

            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage() );
        }

    }
}
