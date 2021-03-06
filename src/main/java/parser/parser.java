package parser;

//Todo Next
//1. function parameters name validity
//2. for / if single statement return
//3. string indexing

//notes:
//evaluation always returns reference
//list clones the elements during its creation
//todo:clone arguments for function
//todo:clone return values

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;

class Parser {

    private Lexer lexer;

    private Node factor() {
        if (this.lexer.finished()) return null;
        String factor = this.lexer.next();
        Node result;
        if (factor.equals("+") || factor.equals("-")) { //unary operator
            Node r = new Node(Node.Type.UNARY_OPERATOR);
            r.lexeme = factor;
            r.children.add(factor());
            return r;
        } else if (factor.equals("(")) {
            result =  goodSolver(0);
            if (result == null) return null;
            if (this.lexer.finished() || !this.lexer.next().equals(")")) {
                System.out.println("missing )");
                return null;
            }
        } else if (factor.equals("[")) {
            //System.out.println("array decl");
            result = new Node(Node.Type.LIST);

            boolean ok = false;
            boolean first = true;

            while (!this.lexer.finished()) {
                if (this.lexer.next().equals("]")) {
                    ok = true;
                    break;
                } else this.lexer.undo();

                if (!first && !this.lexer.next().equals(",")) {
                    System.out.println("expected , not found");
                    return null;
                }

                Node exp = goodSolver(1);
                result.children.add(exp);
                first = false;
            }

            if (!ok) {
                System.out.println("error parsing list, closing brace not found");
                return null;
            }

        } else if (this.lexer.alphabets.contains(factor)) {
            System.out.println("expected identifier or constant but obtained " + factor);
            return null;
        } else { //factor is identifier
            Node.Type type;
            if (factor.charAt(0) == '\"') {
                type = Node.Type.CONSTANT_STRING;
                factor = factor.substring(1, factor.length() - 1);
            } else if (NumberUtils.isDigits(factor)) {
                type = Node.Type.CONSTANT_INTEGER;
            } else if (NumberUtils.isNumber(factor)) {
                type = Node.Type.CONSTANT_FLOAT;
            } else {
                type = Node.Type.IDENTIFIER;
            }
            result = new Node(type);
            result.lexeme = factor;
        }

        //now check any function call () or list indexing []
        boolean fxn = false;
        boolean index = false;
        if (!this.lexer.finished()) {
            String nxt = this.lexer.next();
            this.lexer.undo();
            if (nxt.equals("(")) { // a function call
                fxn = true;
            } else if (nxt.equals("[")) {
                index = true;
            }
        }
        //todo: ()[]()()[][]
        if (index) {
            while (!this.lexer.finished()) {
                if (this.lexer.next().equals("[")) {
                    //System.out.println("deref ");
                    Node ind = goodSolver(0);
                    if (ind == null) return null;
                    if (this.lexer.finished() || !this.lexer.next().equals("]")) {
                        System.out.println("expected ] not found");
                        return null;
                    }
                    Node n = new Node(Node.Type.INDEX);
                    n.children.add(result);
                    n.children.add(ind);
                    result = n;
                } else {
                    this.lexer.undo();
                    break;
                }
            }
        } else if (fxn) {
            this.lexer.next();
            result = new Node(Node.Type.FUNCTION_CALL);
            result.lexeme = factor;
            boolean first = true;

            while (!this.lexer.finished()) {
                String nxt = this.lexer.next();
                this.lexer.undo();
                if (nxt.equals(")")) { // 
                    break;
                }
                if (!first && !this.lexer.next().equals(",")) {
                    System.out.println("expected comma not found");
                    return null;
                }
                Node n = goodSolver(1);
                if (n == null) {
                    System.out.println("error parsing function argument");
                    return null;
                }
                result.children.add(n);
                first = false;
            }

            if (this.lexer.finished() || !this.lexer.next().equals(")")) {
                System.out.println("error: function invoke no closing brace");
                return null;
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
                    System.out.println("error: expected newline not found, obtained " + nxt);
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

        //now check for else block
        boolean ate = false;
        while (!this.lexer.finished()) {
            if (!this.lexer.next().equals("\n")) {
                this.lexer.undo();
                break;
            }
            ate = true;
        }
        if (!this.lexer.finished()) {
            s = this.lexer.next();
            if (!s.equals("else")) {
                this.lexer.undo();
                if (ate) this.lexer.undo(); //undo newline
            } else {
                Node r = interpreter();
                if (r == null) {
                    System.out.println("else block error");
                    return null;
                }
                result.children.add(r);
            }
        }
        return result;
    }

    private Node returnStmt() {
        if (this.lexer.finished()) return null;
        if (!this.lexer.next().equals("return")) return null;
        Node res = new Node(Node.Type.RETURN);
        res.lexeme = "return"; 
        if (!this.lexer.finished()) {
            String nxt = this.lexer.next();
            this.lexer.undo();
            if (!nxt.equals("\n") && !nxt.equals("}")) {
                Node expr = goodSolver(0);
                if (expr == null) return null;
                res.children.add(expr);
            }
        }
        return res;
    }
    
    private Node function_statement() {
        if (this.lexer.finished()) return null;
        String lex = this.lexer.next();
        if (!lex.equals("function")) return null;

        if (this.lexer.finished()) {
            System.out.println("error, missing function name");
            return null;
        } 
        String s = this.lexer.next();
        //todo: perform function name validity
        
        if (this.lexer.finished() || !this.lexer.next().equals("(")) {
            System.out.println("error, function open braces missing");
            return null;
        }

        Node fnc = new Node(Node.Type.FUNCTION_CREATE);
        fnc.lexeme = s; //the function name
        boolean first = true;
        while (!this.lexer.finished()) {
            s = this.lexer.next();
            this.lexer.undo();
            if (s.equals(")")) {
                break;
            }
            if (!first && !this.lexer.next().equals(",")) {
                System.out.println("expected comma not found");
                return null;
            }
            //todo: perform parameter name validity
            Node n = new Node(Node.Type.FUNCTION_PARAM);
            n.lexeme = this.lexer.next();
            fnc.children.add(n);
            first = false;
        }

        if (this.lexer.finished() || !this.lexer.next().equals(")")) {
            System.out.println("error, function no closing braces");
            return null;
        }

        Node blk = matchedBlock();
        if (blk == null) {
            System.out.println("error, parsing function block");
            return null;
        }
        fnc.children.add(blk);
        return fnc;
    }

    private Node def() {
        if (lexer.finished() || !lexer.next().equals("def")) return null;

        Node res = new Node(Node.Type.VAR_DEF);
        res.lexeme = "def"; 
        boolean first = true;
        boolean empty = true;
        while (!this.lexer.finished()) {
            String s = this.lexer.next();
            this.lexer.undo();
            if (s.equals("}") || s.equals("\n")) {
                break;
            }
            if (!first && !this.lexer.next().equals(",")) {
                System.out.println("expected comma not found");
                return null;
            }
            String id = this.lexer.next();
            //todo: perform identiifer validity
            Node n = new Node(Node.Type.IDENTIFIER);
            n.lexeme = id;
            res.children.add(n);
            first = false;
            empty = false;
        }
        if (empty) {
            System.out.println("empty def");
            return null;
        }
        return res;
    }

    //interprets a single non empty statement 
    private Node interpreter() {
        String lex = this.lexer.next();
        this.lexer.undo();
        if (lex.equals("{")) {
            return matchedBlock();
        } else if (lex.equals("def")) {
            return def();
        } else if (lex.equals("return")) {
            return returnStmt();
        } else if (lex.equals("function")) {
            return function_statement();
        } else if (lex.equals("if")) {
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
        leveldel.get(0).add("]"); //for arrays and indices

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
        RunInfo runinfo = new RunInfo();

        HashMap<String, Node> fxnlist = new HashMap<String, Node>();
        try {
            File file = new File("program");
            Scanner input = new Scanner(file);
            String program = "";
            while (input.hasNextLine()) {
                String line = input.nextLine();
                program += line + "\n";

            }
            this.lexer.setData(program);
            if (!this.lexer.process()) {
                System.out.println("parse failed");
            } else {
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
                    if (Node.eval(syntax_tree, runinfo, fxnlist)) {
                        System.out.println("successfully interpreted");
                    } else {
                        System.out.println("interpretation failed");
                    }
                } else {
                    System.out.println("Parse failed");
                }
            }
            input.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //for (String key : fxnlist.keySet()) {
        //    System.out.println(key);
        //}
        
        //add global scope for the interpreter

        runinfo.addScope(new RunInfo.Scope());
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("enter expression");
            String expr = sc.nextLine();
            this.lexer.setData(expr);
            if (!this.lexer.process()) {
                System.out.println("Error: parse failed");
                continue;
            }
            String s = "";
            while (!this.lexer.finished()) {
                s += (s.length() > 0 ? ", " : "") + this.lexer.next();
            }
            //System.out.println(s);
            this.lexer.reset();
            if (this.lexer.finished()) continue;
            Node syntax_tree = this.interpreter();
            if (this.lexer.finished() && syntax_tree != null) {
                System.out.println("Parse successful");
                //syntax_tree.print();
                if (Node.eval(syntax_tree, runinfo, fxnlist)) {
                    System.out.println("successfully interpreted");
                } else {
                    System.out.println("interpretation failed");
                }
            } else {
                if (!this.lexer.finished()) {
                    System.out.println("extra input");
                }
                System.out.println("Parse failed");
            }
        }
    }

}
