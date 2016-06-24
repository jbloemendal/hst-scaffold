package org.onehippo;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    final static Logger log = Logger.getLogger(Route.class);
    public static final Pattern WORD = Pattern.compile("^([A-Za-z]+).*");
    public static final Pattern COMMA = Pattern.compile("^(\\s*,\\s*).*");
    public static final Pattern OPEN_BRACKET = Pattern.compile("^(\\s*\\(\\s*).*");
    public static final Pattern SUB_EXPRESSION = Pattern.compile("(.*\\s*)\\)[^\\)]*$");
    public static final Pattern CLOSE_BRACKET = Pattern.compile("^(\\s*\\)\\s*).*");

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


    /*
    test(foo, bar(baz, blue), duba(dabi, du))
    test(foo, bar(baz, blue)

    EXPR
            WORD
    comp

    IF COMMA
    list.add(component);
    EXPR

    IF OPEN BRACKET
            EXPR
    IF CLOSE BRACKET
    component.add();
    */

    private Component parseComponentExpression(String expression) {
        return parseComponentExpression(null, expression);
    }


    private Component parseComponentExpression(Component parent, String expression) {
        String expr = expression;

        Matcher matcher = WORD.matcher(expr);
        if (!matcher.matches()) {
            return null;
        }

        String word = matcher.group(1);

        Component component = new Component(word);
        if (parent != null) {
            parent.add(component);
        }

        expr = expr.substring(word.length());

        matcher = OPEN_BRACKET.matcher(expr);
        if (matcher.matches()) {

            expr = expr.substring(matcher.group(1).length());
            matcher = SUB_EXPRESSION.matcher(expr);
            if (matcher.matches()) {
                String subExpression = matcher.group(1);
                parseComponentExpression(component, subExpression);
                expr = expr.substring(subExpression.length());
            }

            matcher = CLOSE_BRACKET.matcher(expr);
            if (!matcher.matches()) {
                return null;
            }
            expr = expr.substring(matcher.group(1).length());
        }

        matcher = COMMA.matcher(expr);
        if (matcher.matches()) {
            expr = expr.substring(matcher.group(1).length());
            parseComponentExpression(parent, expr);
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

        log.debug("page construct "+pageConstruct);
        page = parseComponentExpression(pageConstruct);
    }

    public Component getPage() {
        return page;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

}
