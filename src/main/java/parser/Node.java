package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;


public class Node {

    public enum Type {
        BINARY_OPERATOR, IDENTIFIER, CONSTANT, UNARY_OPERATOR, COMMA,
        ASSIGN, IF, BLOCK, FOR, CONSTANT_STRING, FUNCTION_CREATE, FUNCTION_PARAM, FUNCTION_CALL
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

    public String evaluate(RunInfo vars, HashMap<String, Node> fxnlist) {
        if (this.type == Type.FUNCTION_CREATE) {
            fxnlist.put(this.lexeme, this);
            return "void";
        } else if (this.type == Type.IF) {
            //System.out.println("node if" + this.children.get(0).type);
            String cond = this.children.get(0).evaluate(vars, fxnlist);
            if (NumberUtils.createDouble(cond) > 0) {
                return this.children.get(1).evaluate(vars, fxnlist);
            } else {
                return "void";
            }
        } else if (this.type == Type.ASSIGN) {
            String rvalue = this.children.get(1).evaluate(vars, fxnlist);
            //System.out.println("lexeme of lvalue " + this.children.get(0).lexeme);
            vars.setVariable(this.children.get(0).lexeme, rvalue);
            return rvalue;
        } else if (this.type == Type.COMMA) {
            String result = "";
            for (Node i : children) {
                result = i.evaluate(vars, fxnlist);
            }
            return result;
        } else if (this.type == Type.IDENTIFIER) {
            String ret = vars.getValue(this.lexeme);
            if (ret == null) {
                System.out.println("error: undefined variable " + this.lexeme);
                return "error";
            }
            return ret;
        } else if (this.type == Type.CONSTANT) {
            return this.lexeme;
        } else if (this.type == Type.CONSTANT_STRING) {
            return this.lexeme;
        } else if (this.type == Type.BINARY_OPERATOR) {
            String op1 = this.children.get(0).evaluate(vars, fxnlist);
            String op2 = this.children.get(1).evaluate(vars, fxnlist);
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
            String op1 = this.children.get(0).evaluate(vars, fxnlist);
            Double a = NumberUtils.createDouble(op1);
            if (this.lexeme.equals("-")) {
                a = -a;
            }
            return a.toString();
        } else if (this.type == Type.BLOCK) { //must be in
            String res = "void";
            vars.addScope(new RunInfo.Scope());
            for (Node i : this.children) {
                res = i.evaluate(vars, fxnlist);
            }
            vars.popScope();
            return res;
        } else if (this.type == Type.FOR) {
            if (this.children.get(0) != null) this.children.get(0).evaluate(vars, fxnlist); //seed statement
            while (true) {
                String cond = this.children.get(1) == null ? "1.0" : this.children.get(1).evaluate(vars, fxnlist); 
                if (NumberUtils.createDouble(cond) > 0) {
                    this.children.get(3).evaluate(vars, fxnlist); //for execution code
                    if (this.children.get(2) != null) 
                        this.children.get(2).evaluate(vars, fxnlist); //for post execution
                } else {
                    break;
                }
            }
            return "void";
        } else if (this.type == Type.FUNCTION_CALL) {
            if (this.lexeme.equals("print")) {
                //inbuilt function call
                //todo: verify argument size
                String s = this.children.get(0).evaluate(vars, fxnlist);
                if (s.length() > 0 && s.charAt(0) == '"') {
                    s = s.substring(1, s.length() - 1);
                }
                System.out.print(s);
            } else {
                Node function = fxnlist.get(this.lexeme);
                int N = function.children.size();
                RunInfo.Scope sc = new RunInfo.Scope();
                for (int i = 0; i + 1 < N; ++i) {
                    String eval = this.children.get(i).evaluate(vars, fxnlist);
                    sc.variables.put(function.children.get(i).lexeme, eval);
                }

                //insert scope
                vars.addFunctionScope();
                vars.addScope(sc);

                //execute the function block
                function.children.get(N - 1).evaluate(vars, fxnlist);

                //remove scope
                vars.popFunctionScope();
            }
            return "void";
        } else return "need to process";
    }


}
