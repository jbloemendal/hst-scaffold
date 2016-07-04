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

        public String getTemplateFilePath() {
            String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
            String templatePath = HSTScaffold.properties.getProperty(HSTScaffold.TEMPLATE_PATH);
            return projectDir+"/"+templatePath+getComponentPath()+".ftl";
        }

        public String getWebfilePath() {
            return ""+name+"ftl"; // todo
        }

        public String getTemplateName() {
            return name;
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

        public String getComponentPath() {
            StringBuilder componentPath = new StringBuilder();
            Component cursor = this;
            while (cursor != null) {
                componentPath.insert(0, cursor.getName());
                cursor = (cursor.parent);
                if (cursor != null) {
                    componentPath.insert(0, "/");
                }
            }
            return componentPath.toString();
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

        String parameter = null;
        while ((parameter = parameterScanner.findInLine(parameterPattern)) != null) {
            parameter = parameter.substring(1);

            Pattern contentPattern = Pattern.compile(parameter+":(String|Integer|Double|Boolean)");
            Matcher matcher = contentPattern.matcher(contentPath);
            if (matcher.find()) {
                Parameter p = new Parameter();
                p.name = parameter;
                p.type = matcher.group(1);
                parameters.add(p);
                log.debug(String.format("route %s parameter: %s, type %s found.", urlMatcher, p.name, p.type));
            }
        }

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
