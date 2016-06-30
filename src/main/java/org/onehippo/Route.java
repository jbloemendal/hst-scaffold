package org.onehippo;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    final static Logger log = Logger.getLogger(Route.class);

    private String urlMatcher;

    private String contentPath;

    private String pageConstruct;

    private Component page;

    private List<Parameter> parameters = new LinkedList<Parameter>();

    public static class Component {

        private String name;
        private Component parent;
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
            String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
            String templatePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);

            StringBuilder componentPath = new StringBuilder();
            Component cursor = this;
            while (cursor != null) {
                componentPath.insert(0, cursor.getName());
                componentPath.insert(0, "/");
                cursor = (cursor.parent);
            }

            return projectDir+"/"+templatePath+componentPath.toString()+".ftl";
        }

        public String getJavaClass() {
            // todo
            return "Component.java";
        }

        public String getPathJavaClass() {
            String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
            String javaComponentPath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
            return projectDir+"/"+javaComponentPath+"/"+name.substring(0,1).toUpperCase()+name.substring(1)+"Component.java";
        }

        public void add(Component component) {
            component.parent = this;
            this.components.add(component);
        }

        public void add(List<Component> components) {
            this.components.addAll(components);
        }

        public List<Component> getComponents() {
            return this.components;
        }
    }

    public class Parameter {
        public String name;
        public String type;
    }

    public Route(String urlMatcher, String contentPath, String pageConstruct) {
        log.debug("new route "+urlMatcher+", "+contentPath+", "+pageConstruct);
        this.urlMatcher = urlMatcher.trim();
        this.contentPath = contentPath.trim();
        this.pageConstruct = pageConstruct;

        build();
    }


    private void build() {
        log.info("build route "+urlMatcher);

        Scanner parameterScanner = new Scanner(urlMatcher);
        Pattern parameterPattern = Pattern.compile("(\\*|:)[^/]+");

        log.info("find:"+parameterPattern.matcher(urlMatcher).find());

        String parameter = null;
        while ((parameter = parameterScanner.findInLine(parameterPattern)) != null) {
            parameter = parameter.substring(1);

            log.info("route url: "+this.urlMatcher+", parameter: "+parameter);

            Pattern contentPattern = Pattern.compile(parameter+":(String|Integer|Double|Boolean)");
            Matcher matcher = contentPattern.matcher(contentPath);
            if (matcher.find()) {
                log.debug(matcher.group(1));
                Parameter p = new Parameter();
                p.name = parameter;
                p.type = matcher.group(1);
                parameters.add(p);
            }
        }

        log.debug("page construct "+pageConstruct);
        page = ComponentParser.parse(pageConstruct);
    }

    public Component getPage() {
        return page;
    }

    public String getUrl() {
        return this.urlMatcher;
    }

    public String getContentPath() {
        return this.contentPath;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

}
