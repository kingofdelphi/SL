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
            ASSIGN, IF, BLOCK, FOR
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
                //System.out.println("node if" + this.children.get(0).type);
                String cond = this.children.get(0).evaluate(vars);
                if (NumberUtils.createDouble(cond) > 0) {
                    return this.children.get(1).evaluate(vars);
                } else {
                    return "void";
                }
            } else if (this.type == Type.ASSIGN) {
                String rvalue = this.children.get(1).evaluate(vars);
                //System.out.println("lexeme of lvalue " + this.children.get(0).lexeme);
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
                else if (this.lexeme.equals("<=")) result = (a <= b ? 1.0 : 0.0);
                else if (this.lexeme.equals(">")) result = (a > b ? 1.0 : 0.0);
                else if (this.lexeme.equals(">=")) result = (a >= b ? 1.0 : 0.0);
                else if (this.lexeme.equals("==")) result = (a == b ? 1.0 : 0.0);
                else if (this.lexeme.equals("&&")) result = (a != 0 && b != 0 ? 1.0 : 0.0);
                else if (this.lexeme.equals("||")) result = (a != 0 || b != 0 ? 1.0 : 0.0);

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
            } else if (this.type == Type.FOR) {
                if (this.children.get(0) != null) this.children.get(0).evaluate(vars); //seed statement
                while (true) {
                    String cond = this.children.get(1) == null ? "1.0" : this.children.get(1).evaluate(vars); 
                    if (NumberUtils.createDouble(cond) > 0) {
                        this.children.get(3).evaluate(vars); //for execution code
                        if (this.children.get(2) != null) 
                            this.children.get(2).evaluate(vars); //for post execution
                    } else {
                        break;
                    }
                }
                return "void";
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
            Node r =  goodSolver(0);
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

    private void printCurrent() {
        int c = 0;
        String tok = "";
        while (!this.lexer.finished()) {
            tok += "|" + this.lexer.next();
            c++;
        }
        System.out.println(tok);
        while (c-- > 0) this.lexer.undo();
    }

    //matches a sequence of statements
    private Node block() {
        Node result = new Node(Node.Type.BLOCK);

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

    private Node for_statement() {
        if (this.lexer.finished()) return null;
        String lex = this.lexer.next();
        if (!lex.equals("for")) return null;

        if (this.lexer.finished() || !this.lexer.next().equals("(")) {
            System.out.println("error, for statement no open braces");
            return null;
        }
        
        String s = this.lexer.next();
        lexer.undo();

        Node a;
        if (s.equals(";")) a = null; 
        else {
            a = goodSolver(0);
            if (a == null) {
                System.out.println("error, for statement error");
                return null;
            }
        }

        if (this.lexer.finished() || !this.lexer.next().equals(";")) {
            System.out.println("error, for seed statement missing ;");
            return null;
        }

        s = this.lexer.next();
        lexer.undo();

        Node b;
        if (s.equals(";")) b = null; 
        else {
            b = goodSolver(0);

            if (b == null) {
                System.out.println("error, for conditional statement error");
                return null;
            }
        }

        if (this.lexer.finished() || !this.lexer.next().equals(";")) {
            System.out.println("error, for conditional statement missing ;");
            return null;
        }

        s = this.lexer.next();
        lexer.undo();

        Node c;
        if (s.equals(")")) c = null; 
        else {
            c = goodSolver(0);

            if (c == null) {
                System.out.println("error, for post conditional statement error");
                return null;
            }
        }

        if (this.lexer.finished() || !this.lexer.next().equals(")")) {
            System.out.println("error, for statement no closing braces");
            return null;
        }

        s = this.lexer.next();
        this.lexer.undo();

        Node code;
        if (s.equals("{")) code = matchedBlock();
        else code = goodSolver(0);

        if (code == null) {
            System.out.println("error, for block statement error");
            return null;
        }
        Node result = new Node(Node.Type.FOR);
        result.children.add(a);
        result.children.add(b);
        result.children.add(c);
        result.children.add(code);
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
        Node cond = goodSolver(0);
        if (cond == null) {
            System.out.println("error, if block conditional statement error");
            return null;
        }
        if (this.lexer.finished() || !this.lexer.next().equals(")")) {
            System.out.println("error, if statement no closing braces");
            return null;
        }

        String s = this.lexer.next();
        this.lexer.undo();

        Node code;
        if (s.equals("{")) code = matchedBlock();
        else code = goodSolver(0);

        if (code == null) {
            System.out.println("error, if block missing end statement");
            return null;
        }
        Node result = new Node(Node.Type.IF);
        result.children.add(cond);
        result.children.add(code);
        return result;
    }

    //interprets a single non empty statement 
    private Node interpreter() {
        String lex = this.lexer.next();
        this.lexer.undo();

        if (lex.equals("if")) {
            return if_statement();
        } else if (lex.equals("for")) {
            return for_statement();
        }
        return goodSolver(0);
    }

    ArrayList<ArrayList<String>> leveldel;
    ArrayList<ArrayList<String>> levelsep;

    Parser() {
        leveldel = new ArrayList<ArrayList<String>>();
        levelsep = new ArrayList<ArrayList<String>>();

        int levels = 6;
        for (int i = 0; i < levels; ++i) {
            leveldel.add(new ArrayList<String>());
            levelsep.add(new ArrayList<String>());
        }

        levelsep.get(0).add(",");

        levelsep.get(1).add("=");

        levelsep.get(2).add("||");
        levelsep.get(2).add("&&");
        
        levelsep.get(3).add("<");
        levelsep.get(3).add("<=");
        levelsep.get(3).add(">");
        levelsep.get(3).add(">=");
        levelsep.get(3).add("==");

        levelsep.get(4).add("+");
        levelsep.get(4).add("-");

        levelsep.get(5).add("*");
        levelsep.get(5).add("/");

        //delimiters for the lowest level comma
        leveldel.get(0).add("}");
        leveldel.get(0).add(")");
        leveldel.get(0).add("\n");
        leveldel.get(0).add(";");

        for (int i = 1; i < levels; ++i) {
            leveldel.get(i).addAll(levelsep.get(i - 1));
            leveldel.get(i).addAll(leveldel.get(i - 1));
        }

    }

    private Node goodSolver(int level) {
        if (level == 6) return factor();
        //any level needs at least one item, 
        //retrieve the first item of next level
        Node result = goodSolver(level + 1);
        if (result == null) return null;
        if (level == 0) {
            Node tmp = new Node(Node.Type.COMMA);
            tmp.children.add(result);
            result = tmp;
        }
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            boolean finish = false;
            for (String del : this.leveldel.get(level)) {
                if (next.equals(del)) {
                    finish = true;
                    break;
                }
            }
            if (finish) {
                this.lexer.undo();
                break;
            } else {
                boolean donext = false;
                for (String sep : this.levelsep.get(level)) {
                    if (next.equals(sep)) {
                        donext = true;
                        break;
                    }
                }
                if (!donext) {
                    System.out.println("invalid input");
                    return null;
                }
                if (level == 1) { //assignment
                    if (result.type != Node.Type.IDENTIFIER) {
                        System.out.println("error: rvalue where lvalue was expected " + next);
                        return null;
                    }
                }
                Node n;

                if (level == 1) {//assignment operator 
                    //FOR RIGHT ASSOCIATIVITY WE USE
                    //goodSolver(x) = symbol goodSolver(x)
                    n = goodSolver(1);
                } else n = goodSolver(level + 1);

                if (n == null) return null;

                if (level == 0) { //comma level
                    result.children.add(n);
                } else if (level == 1) { //assignment
                    Node root = new Node(Node.Type.ASSIGN);
                    root.lexeme = next;
                    root.children.add(result);
                    root.children.add(n);
                    result = root;
                } else if (level >= 2 && level <= 5) {//< comparison
                    Node root = new Node(Node.Type.BINARY_OPERATOR);
                    root.lexeme = next;
                    root.children.add(result);
                    root.children.add(n);
                    result = root;
                }
            }
        }
        return result;
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
        }
    }

}
