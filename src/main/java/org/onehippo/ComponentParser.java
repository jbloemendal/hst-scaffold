package org.onehippo;

import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
test(foo, bar(baz, blue), duba(dabi, du))
test(foo, bar(baz, blue)

Backus–Naur Form

<expr> ::= <word>(<expr>)|<word>(<expr>), <expr>|<word>
<word> ::= <character>|<word><character>
*/
public class ComponentParser {

    final static Logger log = Logger.getLogger(ComponentParser.class);

    public static final Pattern POINTER = Pattern.compile("^(\\s*\\*)([A-Za-z]+).*");
    public static final Pattern REFERENCE = Pattern.compile("^(\\s*&)([A-Za-z]+).*");
    public static final Pattern WORD = Pattern.compile("^(\\s*)([A-Za-z]+).*");
    public static final Pattern COMMA = Pattern.compile("^(\\s*,\\s*).*");
    public static final Pattern OPEN_BRACKET = Pattern.compile("^(\\s*\\(\\s*).*");
    public static final Pattern SUB_EXPRESSION = Pattern.compile("(.*)\\)[^\\)]*$");
    public static final Pattern CLOSE_BRACKET = Pattern.compile("^(\\s*\\)\\s*).*");

    public static Route.Component parse(String expression) {
        log.debug(String.format("Parsing component expression %s", expression));
        Route.Component root = parseComponentExpression(null, expression);
        return root;
    }

    private static Route.Component parseComponentExpression(Route.Component parent, String expression) {
        String expr = expression;

        boolean reference = false;
        boolean pointer = false;

        Matcher matcher = POINTER.matcher(expr);

        if (matcher.matches()) {
            pointer = true;
        } else {
            matcher = REFERENCE.matcher(expr);
            if (matcher.matches()) {
                reference = true;
            } else {
                matcher = WORD.matcher(expr);
            }
        }

        if (!matcher.matches()) {
            return null;
        }

        String word = matcher.group(2);

        Route.Component component = new Route.Component(word);
        if (parent != null) {
            parent.add(component);
        }

        component.setReference(reference);
        component.setPointer(pointer);

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
