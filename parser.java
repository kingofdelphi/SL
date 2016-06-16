//Todo Next
//1. handle unary operator (+, -)
//2. assignment statement
//
import java.util.*;
import java.io.*;
import org.apache.commons.lang3.math.*;

import static java.util.Arrays.asList;

class Parser {
    private Lexer lexer;

    //Node represents a node in syntax tree
    static class Node {
        public enum Type {
            BINARY_OPERATOR, IDENTIFIER, CONSTANT, UNARY_OPERATOR, COMMA,
            ASSIGN
        }

        public Type type;
        public String lexeme;
        public ArrayList<Node> children;

        Node(Type type) {
            this.type = type;
            this.lexeme = "";
            children = new ArrayList<Node>();
        }

        public void print() {
            for (Node i : children) {
                i.print();
            }
            if (this.type == Type.IDENTIFIER) {
                System.out.println("node: " + this.lexeme);
            } else {
                System.out.println("node: " + this.type);
            }
        }

        public String evaluate(HashMap<String, Double> vars) {
            if (this.type == Type.ASSIGN) {
                String rvalue = this.children.get(1).evaluate(vars);
                //System.out.println("lexeme of lvalue " + this.children.get(0).lexeme);
                //System.out.println("rvalue " + rvalue);
                vars.put(this.children.get(0).lexeme, NumberUtils.toDouble(rvalue));
                return rvalue;
            } else if (this.type == Type.COMMA) {
                String result = "";
                for (Node i : children) {
                    result = i.evaluate(vars);
                }
                return result;
            } else if (this.type == Type.IDENTIFIER) {
                if (!vars.containsKey(this.lexeme)) {
                    System.out.println("error: undefined identifier\n");
                    return "";
                }
                return vars.get(this.lexeme).toString();
            } else if (this.type == Type.CONSTANT) {
                return this.lexeme;
            } else if (this.type == Type.BINARY_OPERATOR) {
                String op1 = this.children.get(0).evaluate(vars);
                String op2 = this.children.get(1).evaluate(vars);
                Double a = NumberUtils.createDouble(op1);
                Double b = NumberUtils.createDouble(op2);
                Double result = 0.0;
                if (this.lexeme.equals("+")) result = a + b;
                else if (this.lexeme.equals("-")) result = a - b;
                else if (this.lexeme.equals("*")) result = a * b;
                else if (this.lexeme.equals("/")) result = a / b;
                return result.toString();
            } else if (this.type == Type.UNARY_OPERATOR) {
                String op1 = this.children.get(0).evaluate(vars);
                Double a = NumberUtils.createDouble(op1);
                if (this.lexeme.equals("-")) {
                    a = -a;
                }
                return a.toString();
            } else return "0";
        }

    }

    private Node factor() {
        if (this.lexer.finished()) return null;
        String factor = this.lexer.next();
        if (factor.equals("+") || factor.equals("-")) { //unary operator
            Node r = new Node(Node.Type.UNARY_OPERATOR);
            r.lexeme = factor;
            r.children.add(factor());
            return r;
        } else if (factor.equals("(")) {
            Node r =  expressionSet();
            if (r == null) return null;
            if (this.lexer.finished() || !this.lexer.next().equals(")")) {
                System.out.println("missing )");
                return null;
            }
            return r;
        } else if (this.lexer.alphabets.contains(factor)) {
            System.out.println("expected identifier or constant but obtained " + factor);
            return null;
        } else { //factor is identifier
            Node.Type type;
            if (NumberUtils.isNumber(factor)) {
                type = Node.Type.CONSTANT;
            } else type = Node.Type.IDENTIFIER;
            Node r = new Node(type);
            r.lexeme = factor;
            return r;
        }
    }

    private Node term() {
        Node r = factor();
        if (r == null) return null;
        //it will take up all factors until a lower precedence operator
        //appears[+,-,)] i.e. eat up all tightly bound factors
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            //lower precedence operator signals end of term
            //
            if (next.equals("=") || next.equals(",") || next.equals("+") || next.equals("-") || next.equals(")")) {
                this.lexer.undo();
                return r;
            }

            if (!next.equals("*") && !next.equals("/")) {
                System.out.println("error unexpected" + next + "|");
                return null;
            }

            Node n = factor();
            if (n == null) return null;
            Node root = new Node(Node.Type.BINARY_OPERATOR);
            root.lexeme = next;
            root.children.add(r);
            root.children.add(n);
            r = root;
        }
        return r;
    }
    
    //expression set
    private Node expressionSet() {
        Node result = new Node(Node.Type.COMMA);
        Node r = expression();
        if (r == null) return null;
        result.children.add(r);
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals(")")) {
                this.lexer.undo();
                break;
            } else if (next.equals(",")) {
                Node n = expression();
                if (n == null) return null;
                result.children.add(n);
            } else {
                return null; //undefined symbol
            }
        }
        return result;
    }
    
    private Node expression() {
        Node result =  simple_expression();
        if (result == null) return null;
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals("=")) {
                if (result.type != Node.Type.IDENTIFIER) return null;
            } else if (next.equals(")") || next.equals(",")) {
                this.lexer.undo();
                break;
            }
            else return null;
            Node r = expression();
            if (r == null) return null;
            Node root = new Node(Node.Type.ASSIGN);
            root.lexeme = "=";
            root.children.add(result);
            root.children.add(r);
            result = root;
        }
        return result;
    }

    //simple expression is a sequence of terms added or subtracted
    private Node simple_expression() {
        Node r = term();
        if (r == null) {
            System.out.println("invalid expression");
            return null;
        }
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals("=") || next.equals(",") || next.equals(")")) {
                this.lexer.undo();
                break;
            }
            if (!next.equals("+") && !next.equals("-")) {
                System.out.println("error unexpected |" + next + "|");
                return null;
            }
            Node n = term();
            if (n == null) return null;
            Node root = new Node(Node.Type.BINARY_OPERATOR);
            root.lexeme = next;
            root.children.add(r);
            root.children.add(n);
            r = root;
        }
        return r;
    }

    public void main() {
        this.lexer = new Lexer();
        HashMap<String, Double> vars = new HashMap<String, Double>();
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("enter expression");
            String expr = sc.nextLine();
            this.lexer.setData(expr);
            this.lexer.process();
            String s = "";
            while (!this.lexer.finished()) {
                s += (s.length() > 0 ? ", " : "") + this.lexer.next();
            }
            System.out.println(s);
            this.lexer.reset();
            System.out.println(expr);
            Node syntax_tree = this.expressionSet();
            if (syntax_tree != null) {
                System.out.println("Parse successful\n");
                syntax_tree.print();
                System.out.println(syntax_tree.evaluate(vars));
            } else {
                System.out.println("Parse failed\n");
            }
        }
    }

}
