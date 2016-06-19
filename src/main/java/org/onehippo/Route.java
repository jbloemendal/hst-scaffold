package org.onehippo;


import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    private String urlMatcher;

    private String contentPath;

    private String pageConstruct;

    private Component page;

    private List<String> parameters;

    public class Component {
        private String name;
        private List<Component> components;

        public Component(String name) {
            this.name = name;
            this.components = new ArrayList<Component>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTemplate() {
            return name; // todo path
        }

        public String getJavaClass() {
            return name; // todo path
        }

        public void add(Component component) {
            this.components.add(component);
        }

        public void add(List<Component> components) {
            this.components.addAll(components);
        }

        public List<Component> getComponents() {
            return this.components;
        }
    }

    public Route(String urlMatcher, String contentPath, String pageConstruct) {
        this.urlMatcher = urlMatcher;
        this.contentPath = contentPath;
        this.pageConstruct = pageConstruct;

        build();
    }

    private Component parseComponentExpression(String expr) {
        Component component = null;

        Pattern pattern = Pattern.compile("([^(]+)(\\((.*)\\))?$");
        Matcher matcher = pattern.matcher(expr);
        if (matcher.matches()) {
            component = new Component(matcher.group(1));
            if (StringUtils.isNoneEmpty(matcher.group(3))) {
                component.add(parseComponentExpression(matcher.group(3)));
            }
        }

        return component;
    }

    private void build() {
        Scanner parameterScanner = new Scanner(urlMatcher);

        Pattern parameterPattern = Pattern.compile("\\*|:[^/]]+");
        while (parameterScanner.hasNext(parameterPattern)) {
            String parameter = parameterScanner.next(parameterPattern);
            parameters.add(parameter);
        }

        page = parseComponentExpression(pageConstruct);
    }

    public Component getPage() {
        return page;
    }

    public List<String> getParameters() {
        return parameters;
    }

    // todo parse placehodlers
    // *name, :id
    // /text/*path       /contact/path:String    text(header,main(banner, text),footer)
}
