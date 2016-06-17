//Todo Next
//1. handle unary operator (+, -)
//2. assignment statement
//

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

import org.apache.commons.lang3.math.*;

import static java.util.Arrays.asList;

class Parser {
    private Lexer lexer;

    //Node represents a node in syntax tree
    static class Node {

        public enum Type {
            BINARY_OPERATOR, IDENTIFIER, CONSTANT, UNARY_OPERATOR, COMMA,
            ASSIGN, IF, BLOCK
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
            if (this.type == Type.IF) {
                String cond = this.children.get(0).evaluate(vars);
                if (NumberUtils.createDouble(cond) > 0) {
                    return this.children.get(1).evaluate(vars);
                } else {
                    return "void";
                }
            } else if (this.type == Type.ASSIGN) {
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
                    System.out.println("error: undefined identifier " + this.lexeme);
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
                else if (this.lexeme.equals("<")) result = (a < b ? 1.0 : 0.0);
                return result.toString();
            } else if (this.type == Type.UNARY_OPERATOR) {
                String op1 = this.children.get(0).evaluate(vars);
                Double a = NumberUtils.createDouble(op1);
                if (this.lexeme.equals("-")) {
                    a = -a;
                }
                return a.toString();
            } else if (this.type == Type.BLOCK) {
                String res = "void";
                for (Node i : this.children) {
                    res = i.evaluate(vars);
                }
                return res;
            } else return "need to process";
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
            if (next.equals("\n") || next.equals("}") || next.equals("<") || next.equals("=") || next.equals(",") || next.equals("+") || next.equals("-") || next.equals(")")) {
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
            if (next.equals("\n") || next.equals("}") || next.equals(")")) {
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

    private Node matchedBlock() {
        if (this.lexer.finished()) return null;
        String lex = this.lexer.next();
        if (!lex.equals("{")) return null;
        
        Node result = block();
        
        if (result == null) return null;
        if (this.lexer.finished() || !this.lexer.next().equals("}")) {
            System.out.println("block statement unmatched");
            return null;
        }
        return result;
    }

    //matches a sequence of statements
    private Node block() {
        Node result = new Node(Node.Type.BLOCK);
        //int c = 0;
        //String tok = "";
        //while (!this.lexer.finished()) {
        //    tok += "|" + this.lexer.next();
        //    c++;
        //}
        //System.out.println(tok);
        //while (c-- > 0) this.lexer.undo();

        while (!this.lexer.finished()) {
            String nxt = this.lexer.next();
            if (nxt.equals("\n")) continue;
            else this.lexer.undo();
            if (nxt.equals("}")) break;
            Node cond = interpreter();
            if (cond == null) return null;
            if (!this.lexer.finished()) {
                nxt = this.lexer.next();
                this.lexer.undo();
                if (!nxt.equals("}") && !nxt.equals("\n")) {
                    System.out.println("error: expected newline not found");
                    return null;
                }
            }
            result.children.add(cond);
        }
        return result;
    }

    private Node if_statement() {
        if (this.lexer.finished()) return null;
        String lex = this.lexer.next();
        if (!lex.equals("if")) return null;

        if (this.lexer.finished() || !this.lexer.next().equals("(")) {
            System.out.println("error, if statement no open braces");
            return null;
        }
        Node cond = expression();
        if (cond == null) {
            System.out.println("error, if block missing conditional statement");
            return null;
        }
        if (this.lexer.finished() || !this.lexer.next().equals(")")) {
            System.out.println("error, if statement no closing braces");
            return null;
        }

        String s = this.lexer.next();
        this.lexer.undo();

        if (s.equals("{")) return matchedBlock();

        Node code = expression();
        if (code == null) {
            System.out.println("error, if block missing end statement");
            return null;
        }
        Node result = new Node(Node.Type.IF);
        result.children.add(cond);
        result.children.add(code);
        return result;
    }

    private Node lexp() {
        Node result =  simple_expression();
        if (result == null) return null;
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals("<")) {
                Node r = simple_expression();
                if (r == null) {
                    System.out.println("error: missing rvalue for " + next);
                    return null;
                }
                Node root = new Node(Node.Type.BINARY_OPERATOR);
                root.lexeme = "<";
                root.children.add(result);
                root.children.add(r);
                result = root;
            } else if (next.equals("\n") || next.equals("}") || next.equals("=") || next.equals(")") || next.equals(",")) {
                this.lexer.undo();
                break;
            } else return null;
        }
        return result;
    }

    private Node expression() {
        Node result =  lexp();
        if (result == null) return null;
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals("=")) {
                if (result.type != Node.Type.IDENTIFIER) {
                    System.out.println("error: rvalue where lvalue was expected " + next);
                    return null;
                }
            } else if (next.equals("\n") || next.equals("}") || next.equals(")") || next.equals(",")) {
                this.lexer.undo();
                break;
            } else return null;

            Node r = expression();
            if (r == null) {
                System.out.println("error: missing rvalue for " + next);
                return null;
            }
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
            if (next.equals("\n") || next.equals("}") || next.equals("=") || next.equals(",") || next.equals(")") || next.equals("<")) {
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

    //interprets a single non empty statement 
    private Node interpreter() {
        String lex = this.lexer.next();
        this.lexer.undo();

        if (lex.equals("if")) {
            return if_statement();
        }
        return expressionSet();
    }

    private Node build() {
        Node r = block();
        if (!lexer.finished()) {
            System.out.println("error: extra input");
            return null;
        }
        return r;
    }


    public void main() {
        this.lexer = new Lexer();
        HashMap<String, Double> vars = new HashMap<String, Double>();
        try {
            File file = new File("program");
            Scanner input = new Scanner(file);
            String program = "";
            while (input.hasNextLine()) {
                String line = input.nextLine();
                program += line + "\n";
            }
            this.lexer.setData(program);
            this.lexer.process();
            String s = "";
            while (!this.lexer.finished()) {
                s += (s.length() > 0 ? ", " : "") + this.lexer.next();
            }
            System.out.println(s);
            this.lexer.reset();
            Node syntax_tree = this.build();
            if (syntax_tree != null) {
                System.out.println("Parse successful");
                //syntax_tree.print();
                System.out.println("Result: " + syntax_tree.evaluate(vars));
            } else {
                System.out.println("Parse failed");
            }
            input.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            //System.out.println(s);
            this.lexer.reset();
            Node syntax_tree = this.build();
            if (syntax_tree != null) {
                System.out.println("Parse successful");
                //syntax_tree.print();
                System.out.println("Result: " + syntax_tree.evaluate(vars));
            } else {
                System.out.println("Parse failed");
            }
        }
    }

}
