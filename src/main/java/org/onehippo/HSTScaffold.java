package org.onehippo;


import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class HSTScaffold {

    private static HSTScaffold scaffold;

    private List<Route> routes;

    public List<Route> getRoutes() {

        return routes;
    }

    private void read() {
        File config = new File("scaffold.hst"); // todo file path

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
