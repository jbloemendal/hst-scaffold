package org.onehippo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
public class ComponentParser {

    public static final Pattern WORD = Pattern.compile("^(\\s*)([A-Za-z]+).*");
    public static final Pattern COMMA = Pattern.compile("^(\\s*,\\s*).*");
    public static final Pattern OPEN_BRACKET = Pattern.compile("^(\\s*\\(\\s*).*");
    public static final Pattern SUB_EXPRESSION = Pattern.compile("(.*)\\)[^\\)]*$");
    public static final Pattern CLOSE_BRACKET = Pattern.compile("^(\\s*\\)\\s*).*");

    public static Route.Component parse(String expression) {
        return parseComponentExpression(null, expression);
    }

    private static Route.Component parseComponentExpression(Route.Component parent, String expression) {
        String expr = expression;

        Matcher matcher = WORD.matcher(expr);
        if (!matcher.matches()) {
            return null;
        }

        String word = matcher.group(2);

        Route.Component component = new Route.Component(word);
        if (parent != null) {
            parent.add(component);
        }

        expr = expr.substring(matcher.group(1).length()+matcher.group(2).length());

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

}
