package org.onehippo;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class HSTScaffold {

    final public static String SCAFFOLD_DIR_NAME = ".scaffold";

    private static HSTScaffold scaffold;

    private List<Route> routes;

    public List<Route> getRoutes() {

        return routes;
    }

    private void read() {
        File config = new File("scaffold.hst"); // todo file path

        try {
            Scanner scanner = new Scanner(config);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                Scanner lineScanner = new Scanner(line);
                if (lineScanner.hasNext(Pattern.compile("^\\s*#"))) {
                    break;
                }

                // *name, :id
                // /text/*path       /contact/path:String    text(header,main(banner, text),footer)
                Pattern urlPattern = Pattern.compile("\\s*(/[^/]]/?)+"); // todo
                if (!lineScanner.hasNext(urlPattern)) {
                    continue; // todo error logging
                }
                String urlMatcher = lineScanner.next(urlPattern);

                Pattern contentPattern = Pattern.compile("\\s*(/[^/]]/?)+"); // todo
                if (!lineScanner.hasNext(contentPattern)) {
                    continue; // todo error logging
                }
                String contentPath = lineScanner.next(contentPattern);

                Pattern pagePattern = Patter.compile(""); //todo
                if (!lineScanner.hasNext(pagePattern)) {
                    continue; // todo error logging
                }
                String pageConstruct = lineScanner.next(pagePattern);

                routes.add(new Route(urlMatcher, contentPath, pageConstruct));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // todo
        }

        // todo file scanner / reader
        // fill routes, pages, components ...
    }

    public static HSTScaffold instance() {
        if (scaffold == null) {
            scaffold = new HSTScaffold();
        }
        return scaffold;
    }

}
