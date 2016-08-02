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
        private boolean reference = false;
        private boolean pointer = false;
        private boolean inconsistent;

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
            return projectDir+"/"+templatePath+"/"+getComponentPath()+".ftl";
        }

        public String getWebfilePath() {
            String projectName = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_NAME);
            String basePath = HSTScaffold.properties.getProperty(HSTScaffold.WEBFILE_BASE_PATH);
            return basePath+projectName+"/"+getComponentPath()+".ftl";
        }

        public String getTemplateName() {
            return name;
        }

        public String getJavaClass() {
            String packagePath = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_PACKAGE_NAME);
            return packagePath+"."+StringUtils.capitalize(name)+"Component";
        }

        public String getPathJavaClass() {
            String projectDir = HSTScaffold.properties.getProperty(HSTScaffold.PROJECT_DIR);
            String javaComponentPath = HSTScaffold.properties.getProperty(HSTScaffold.JAVA_COMPONENT_PATH);
            return projectDir+"/"+javaComponentPath+"/"+StringUtils.capitalize(name)+"Component.java";
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

        public Component getComponent(String name) {
            for (Component component : this.components) {
                if (component.getName().equals(name)) {
                    return component;
                }
            }
            return null;
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

        public void setReference(boolean arg0) {
            this.reference = arg0;
        }

        public boolean isReference() {
            return this.reference;
        }

        public boolean isPointer() {
            return pointer;
        }

        public boolean isReferenced() {
            return false;
        }

        public void setPointer(boolean pointer) {
            this.pointer = pointer;
        }

        public Component getParent() {
            return parent;
        }

        public void setInconsistent(boolean inconsistent) {
            this.inconsistent = inconsistent;
        }

        public boolean isInconsistent() {
            return this.inconsistent;
        }

        public List<Component> getParents() {
            List<Component> path = new LinkedList<Component>();

            Route.Component cursor = this;
            while (cursor != null) {
                cursor = (cursor.getParent());
                if (cursor == null) {
                    continue;
                }
                path.add(0, cursor);
            }

            return path;
        }

        public List<Component> getReferenceBranch() {
            List<Component> branch = new LinkedList<Component>();

            // skip nodes till reference found
            List<Component> parents = getParents();
            while (parents.size() > 0) {
                if (parents.get(0).isReference()) {
                    break;
                }
                parents.remove(0);
            }

            // build branch
            while (parents.size() > 0) {
                Route.Component parent = parents.remove(0);
                branch.add(parent);
            }

            return branch;
        }

        public String getReferenceBranchPath() {
            StringBuilder stringBuilder = new StringBuilder();
            List<Component> branch = getReferenceBranch();
            for (Component component : branch) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("/");
                }
                stringBuilder.append(component.getName());
            }
            return stringBuilder.toString();
        }

        public Component getPage() {
            Route.Component cursor = this;
            while (cursor.getParent() != null) {
                cursor = (cursor.getParent());
            }

            return cursor;
        }

        public boolean isLeaf() {
            return this.components.isEmpty();
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

    public void setPage(Component page) {
        this.page = page;
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

    public String getPageConstruct() {
        return pageConstruct;
    }
}
