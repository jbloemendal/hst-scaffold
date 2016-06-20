package org.onehippo;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    final static Logger log = Logger.getLogger(Route.class);
    public static final Pattern LETTER = Pattern.compile("[A-Za-z]]");
    public static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
    public static final Pattern OPEN_BRACKET = Pattern.compile("\\(");

    private String urlMatcher;

    private String contentPath;

    private String pageConstruct;

    private Component page;

    private List<Parameter> parameters = new LinkedList<Parameter>();

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

        log.debug("parseComponentExpression: " + expr);

        Pattern pattern = Pattern.compile("^([^(,\\s]+)\\(([\\)]*\\))"); // todo fix expression
        Scanner scanner = new Scanner(expr);

        String componentStr = null;
        while ((componentStr = scanner.findInLine(pattern)) != null) {
            log.debug("!!"+componentStr);

            Matcher matcher = pattern.matcher(componentStr);
            if (!matcher.matches()) {
                log.debug("continue");
                continue;
            }

            if (component == null) {
                component = new Component(matcher.group(1));
            } else {
                component.add(new Component(matcher.group(1)));
            }


            if (StringUtils.isNoneEmpty(matcher.group(3))) { // subgroup
                log.debug("foo");
                String subExpr = matcher.group(3);
                component.add(parseComponentExpression(subExpr));
            }
            log.debug("hasNext:"+scanner.hasNext(pattern));
        }
        return component;
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

        page = parseComponentExpression(pageConstruct);
    }

    public Component getPage() {
        return page;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

}
