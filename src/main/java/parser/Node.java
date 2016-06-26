package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;


public class Node {

    public enum Type {
        BINARY_OPERATOR, IDENTIFIER, CONSTANT_INTEGER, CONSTANT_FLOAT, UNARY_OPERATOR, COMMA,
        ASSIGN, IF, BLOCK, FOR, CONSTANT_STRING, FUNCTION_CREATE, FUNCTION_PARAM, FUNCTION_CALL, VAR_DEF,
        RETURN
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
        if (this.type == Type.RETURN) {
            if (this.children.size() > 0) {
                Return val = this.children.get(0).evaluate(vars, fxnlist);
                return new Return("return", val.eval(vars));
            }
            return new Return("return");
        } else if (this.type == Type.VAR_DEF) {
            //add a new variable initialized to null
            //System.out.println("define variable" + this.children.get(0).lexeme);
            for (Node i : this.children) {
                vars.createVariable(i.lexeme);
            }
            return new Return();
        } else if (this.type == Type.FUNCTION_CREATE) {
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
            if (!vars.variableExists(name)) {
                System.out.println("error: undefined variable " + name);
                return null;
            }
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
            Rvalue rval = vars.getValue(this.lexeme);
            if (rval == null) {
                System.out.println("error: undefined variable " + this.lexeme);
                return null;
            }
            //System.out.println("variable " + this.lexeme + " value = " + rval.data);
            return new Return(null, rval);
        } else if (this.type == Type.CONSTANT_INTEGER) {
            Rvalue rval = new Rvalue("integer");
            rval.data = this.lexeme;
            return new Return(null, rval);
        } else if (this.type == Type.CONSTANT_FLOAT) {
            Rvalue rval = new Rvalue("float");
            rval.data = this.lexeme;
            return new Return(null, rval);
        } else if (this.type == Type.CONSTANT_STRING) {
            Rvalue rval = new Rvalue("string");
            rval.data = this.lexeme;
            return new Return(null, rval);
        } else if (this.type == Type.BINARY_OPERATOR) {
            Return op1 = this.children.get(0).evaluate(vars, fxnlist);
            Return op2 = this.children.get(1).evaluate(vars, fxnlist);

            String dtype;

            if (op1.value.type.equals("integer") && op2.value.type.equals("integer")) dtype = "integer";
            else if (op1.value.type.equals("float") || op2.value.type.equals("float")) dtype = "float";
            else {
                System.out.println("error: binary operation on unmatched types");
                return null;
            }

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
            
            Rvalue rval = new Rvalue(dtype);

            if (dtype.equals("integer")) rval.data = Integer.toString(result.intValue());
            else rval.data = result.toString();
            return new Return(null, rval);

        } else if (this.type == Type.UNARY_OPERATOR) {
            Return op1 = this.children.get(0).evaluate(vars, fxnlist);
            Double a = NumberUtils.createDouble(op1.eval(vars).data);
            if (this.lexeme.equals("-")) {
                a = -a;
            }
            Rvalue rval = new Rvalue(op1.value.type);
            rval.data = a.toString();
            return new Return(null, rval);
        } else if (this.type == Type.BLOCK) { //must be in
            vars.addScope(new RunInfo.Scope());
            Return ret = new Return();
            for (Node i : this.children) {
                Return r = i.evaluate(vars, fxnlist);
                if (r.name != null && r.name.equals("return")) {
                    ret = r;
                    //System.out.println("return fxn " + r.value);
                    break;
                }
            }
            vars.popScope();
            return ret;
        } else if (this.type == Type.FOR) {
            if (this.children.get(0) != null) this.children.get(0).evaluate(vars, fxnlist); //seed statement
            Return ret = new Return();
            while (true) {
                String cond = this.children.get(1) == null ? "1.0" : this.children.get(1).evaluate(vars, fxnlist).eval(vars).data; 
                if (NumberUtils.createDouble(cond) == 0) break;
                Return r = this.children.get(3).evaluate(vars, fxnlist); //for execution code
                if (r.name != null && r.name.equals("return")) {
                    //System.out.println("exit from fxn");
                    ret = r;
                    break;
                }
                if (this.children.get(2) != null) {
                    this.children.get(2).evaluate(vars, fxnlist); //for post execution
                }
            }
            return ret;
        } else if (this.type == Type.FUNCTION_CALL) {
            if (this.lexeme.equals("print")) {
                //inbuilt function call
                //todo: verify argument size
                String s = this.children.get(0).evaluate(vars, fxnlist).eval(vars).data;
                if (s.length() > 0 && s.charAt(0) == '"') {
                    s = s.substring(1, s.length() - 1);
                }
                System.out.print(s);
                return new Return();
            } else {
                //System.out.println("function call " + this.lexeme);
                Node function = fxnlist.get(this.lexeme);
                int N = function.children.size();
                RunInfo.Scope sc = new RunInfo.Scope();

                for (int i = 0; i + 1 < N; ++i) {
                    Rvalue rval = this.children.get(i).evaluate(vars, fxnlist).eval(vars);
                    if (rval == null) {
                        System.out.println("error: Null returned fxn argu");
                    }
                    String name = function.children.get(i).lexeme;
                    sc.variables.put(name, rval);
                }

                //insert scope
                vars.addFunctionScope();
                vars.addScope(sc);

                //execute the function block
                Return r = function.children.get(N - 1).evaluate(vars, fxnlist);
                Return ret = new Return();
                if (r.name != null && r.name.equals("return")) {
                    ret = new Return(null, r.value);
                }
                //System.out.println("exited from function");
                //remove scope
                vars.popFunctionScope();
                return ret;
            }
        } else return new Return();
    }
}
