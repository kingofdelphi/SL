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

    public Return evaluate(RunInfo vars, HashMap<String, Node> fxnlist) {
        if (this.type == Type.FUNCTION_CREATE) {
            fxnlist.put(this.lexeme, this);
            return new Return();
        } else if (this.type == Type.IF) {
            //System.out.println("node if" + this.children.get(0).type);
            Return cond = this.children.get(0).evaluate(vars, fxnlist);
            if (NumberUtils.createDouble(cond.eval(vars).data) > 0) {//*****************************************
                return this.children.get(1).evaluate(vars, fxnlist);
            } else {
                return new Return();
            }
        } else if (this.type == Type.ASSIGN) {
            Return rvalue = this.children.get(1).evaluate(vars, fxnlist);
            String name = this.children.get(0).lexeme;
            Rvalue var = rvalue.eval(vars);
            //System.out.println("variable assign " + this.children.get(0).lexeme);
            vars.setVariable(this.children.get(0).lexeme, var);
            return new Return(name);
        } else if (this.type == Type.COMMA) {
            Return result = null;
            for (Node i : children) {
                result = i.evaluate(vars, fxnlist);
            }
            return result;
        } else if (this.type == Type.IDENTIFIER) {
            //System.out.println("variable " + this.lexeme);
            Rvalue rval = vars.getValue(this.lexeme);
            if (rval == null) {
                //System.out.println("error: undefined variable " + this.lexeme);
                return null;
            }
            //System.out.println("variable " + this.lexeme + " value = " + rval.data);
            return new Return(null, rval);
        } else if (this.type == Type.CONSTANT) {
            Rvalue rval = new Rvalue("integer");
            rval.data = this.lexeme;
            return new Return(null, rval);
        } else if (this.type == Type.CONSTANT_STRING) {
            Rvalue rval = new Rvalue("string");
            rval.data = this.lexeme;
            return new Return(null, rval);
        } else if (this.type == Type.BINARY_OPERATOR) {
            Return op1 = this.children.get(0).evaluate(vars, fxnlist);
            Return op2 = this.children.get(1).evaluate(vars, fxnlist);
            Double a = NumberUtils.createDouble(op1.eval(vars).data);
            Double b = NumberUtils.createDouble(op2.eval(vars).data);
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

            Rvalue rval = new Rvalue("string");
            rval.data = result.toString();
            return new Return(null, rval);
        } else if (this.type == Type.UNARY_OPERATOR) {
            Return op1 = this.children.get(0).evaluate(vars, fxnlist);
            Double a = NumberUtils.createDouble(op1.eval(vars).data);
            if (this.lexeme.equals("-")) {
                a = -a;
            }
            Rvalue rval = new Rvalue("integer");
            rval.data = a.toString();
            return new Return(null, rval);
        } else if (this.type == Type.BLOCK) { //must be in
            vars.addScope(new RunInfo.Scope());
            for (Node i : this.children) {
                i.evaluate(vars, fxnlist);
            }
            vars.popScope();
            return new Return(); //void return
        } else if (this.type == Type.FOR) {
            if (this.children.get(0) != null) this.children.get(0).evaluate(vars, fxnlist); //seed statement
            while (true) {
                String cond = this.children.get(1) == null ? "1.0" : this.children.get(1).evaluate(vars, fxnlist).eval(vars).data; 
                if (NumberUtils.createDouble(cond) > 0) {
                    this.children.get(3).evaluate(vars, fxnlist); //for execution code
                    if (this.children.get(2) != null) 
                        this.children.get(2).evaluate(vars, fxnlist); //for post execution
                } else {
                    break;
                }
            }
            return new Return(); //void return
        } else if (this.type == Type.FUNCTION_CALL) {
            if (this.lexeme.equals("print")) {
                //inbuilt function call
                //todo: verify argument size
                String s = this.children.get(0).evaluate(vars, fxnlist).eval(vars).data;
                if (s.length() > 0 && s.charAt(0) == '"') {
                    s = s.substring(1, s.length() - 1);
                }
                System.out.print(s);
            } else {
                Node function = fxnlist.get(this.lexeme);
                int N = function.children.size();
                RunInfo.Scope sc = new RunInfo.Scope();
                for (int i = 0; i + 1 < N; ++i) {
                    String eval = this.children.get(i).evaluate(vars, fxnlist).eval(vars).data;
                    String name = function.children.get(i).lexeme;
                    Rvalue var = new Rvalue("integer");
                    var.data = eval;
                    sc.variables.put(name, var);
                }

                //insert scope
                vars.addFunctionScope();
                vars.addScope(sc);

                //execute the function block
                function.children.get(N - 1).evaluate(vars, fxnlist);

                //remove scope
                vars.popFunctionScope();
            }
            return new Return();
        } else return new Return();
    }
}
