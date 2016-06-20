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

    private List<Parameter> parameters;

    public class Parameter {
        public String name;
        public String type;
    }

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
            String projectDir = HSTScaffold.getConfig().getProperty(HSTScaffold.PROJECT_DIR);
            String templatePath = HSTScaffold.getConfig().getProperty(HSTScaffold.TEMPLATE_PATH);
            return projectDir+templatePath+name;
        }

        public String getJavaClass() {
            String projectDir = HSTScaffold.getConfig().getProperty(HSTScaffold.PROJECT_DIR);
            String javaComponentPath = HSTScaffold.getConfig().getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
            return projectDir+javaComponentPath+name;
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
            String parameter = parameterScanner.next(parameterPattern).substring(1);

            Pattern contentPattern = Pattern.compile(parameter+":(String|Integer|Double|Boolean)");
            Matcher matcher = contentPattern.matcher(contentPath);
            if (matcher.matches()) {
                Parameter p = new Parameter();
                p.name = parameter;
                p.type = matcher.group(1);
                parameters.add(p);
            }
        }

        page = parseComponentExpression(pageConstruct);
    }

    public Component getPage() {
        return page;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

}
