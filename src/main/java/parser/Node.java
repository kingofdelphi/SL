package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;

class RunException extends Exception {
    RunException(String s) {
        super(s);
    }
}

public class Node {

    public enum Type {
        BINARY_OPERATOR, IDENTIFIER, CONSTANT_INTEGER, CONSTANT_FLOAT, UNARY_OPERATOR, COMMA,
        ASSIGN, IF, BLOCK, FOR, CONSTANT_STRING, FUNCTION_CREATE, FUNCTION_PARAM, FUNCTION_CALL, VAR_DEF,
        RETURN
    }

    static boolean isArith(String op) {
        return op.equals("+") ||
            op.equals("-") ||
            op.equals("*") ||
            op.equals("/");
    }

    static boolean isComp(String op) {
        return op.equals("<") ||
            op.equals("<=") ||
            op.equals(">") ||
            op.equals(">=") ||
            op.equals("==");
    }


    static Double doArith(String op, Double a, Double b) {
        Double result = null;
        if (op.equals("+")) result = a + b;
        else if (op.equals("-")) result = a - b;
        else if (op.equals("*")) result = a * b;
        else if (op.equals("/")) result = a / b;
        return result;
    }

    static <T extends Comparable> int doRel(String op, T a, T b) {
        boolean result = false;
        int dist = a.compareTo(b);
        if (op.equals("<")) result = dist < 0;
        else if (op.equals("<=")) result = dist <= 0;
        else if (op.equals(">")) result = dist > 0;
        else if (op.equals(">=")) result = dist >= 0;
        else if (op.equals("==")) result = dist == 0;
        return result ? 1 : 0;
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

    public static boolean eval(Node n, RunInfo vars, HashMap<String, Node> fxnlist) {
        boolean ok = true;
        try {
            n.evaluate(vars, fxnlist);
        } catch (RunException e) {
            System.out.println("Error: " + e.getMessage());
            ok = false;
        }
        return ok;
    }

    public Return evaluate(RunInfo vars, HashMap<String, Node> fxnlist) throws RunException {
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
            if (cond.eval(vars) == null) {
                throw new RunException("evaluation of void expression");
            }
            Return rv = new Return();
            if (NumberUtils.createDouble(cond.eval(vars).data) > 0) {//*****************************************
                rv = this.children.get(1).evaluate(vars, fxnlist);
            } else if (this.children.size() == 3) {
                //this is the else statement
                rv = this.children.get(2).evaluate(vars, fxnlist);
            }
            return rv;
        } else if (this.type == Type.ASSIGN) {
            Return rvalue = this.children.get(1).evaluate(vars, fxnlist);
            String name = this.children.get(0).lexeme;
            if (!vars.variableExists(name)) {
                throw new RunException("undefined variable " + name);
            }
            Rvalue var = rvalue.eval(vars);
            if (var == null) {
                throw new RunException("assignment of void expression");
            }
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
            if (vars.variableExists(this.lexeme)) {
                Rvalue rval = vars.getValue(this.lexeme);
                if (rval == null) {
                    throw new RunException("uninitialized variable " + this.lexeme);
                }
                //System.out.println("variable " + this.lexeme + " value = " + rval.data);
                return new Return(null, rval);
            } else {
                throw new RunException("undefined variable " + this.lexeme);
            }
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

            if (op1.eval(vars) == null || op2.eval(vars) == null) {
                throw new RunException("operation " + this.lexeme + " on void operands");
            }

            String dtype;

            if (op1.value.type.equals("integer") && op2.value.type.equals("integer")) dtype = "integer";
            else if (op1.value.type.equals("integer") && op2.value.type.equals("float")) dtype = "float";
            else if (op1.value.type.equals("float") && op2.value.type.equals("integer")) dtype = "float";
            else if (op1.value.type.equals("float") && op2.value.type.equals("float")) dtype = "float";
            else if (op1.value.type.equals("string") && op2.value.type.equals("string")) dtype = "string";
            else {
                throw new RunException("operation " + this.lexeme + " on unmatched types ");
            }
            
            if (isArith(this.lexeme)) {
                Rvalue rval = new Rvalue(dtype);
                if (dtype.equals("integer")) {
                    Integer a = Integer.parseInt(op1.eval(vars).data);
                    Integer b = Integer.parseInt(op2.eval(vars).data);
                    Integer result = doArith(this.lexeme, new Double(a), new Double(b)).intValue();
                    rval.data = result.toString();
                } else if (dtype.equals("float")) {
                    Double a = NumberUtils.createDouble(op1.eval(vars).data);
                    Double b = NumberUtils.createDouble(op2.eval(vars).data);
                    Double result = doArith(this.lexeme, a, b);
                    rval.data = result.toString();
                } else if (dtype.equals("string")) {
                    if (this.lexeme.equals("+")) {
                        String a = op1.eval(vars).data;
                        String b = op2.eval(vars).data;
                        rval.data = a + b;
                    } else {
                        throw new RunException("unsupported operation " + this.lexeme + " on string");
                    }
                } else {
                    throw new RunException("unsupported type " + dtype);
                }
                return new Return(null, rval);
            } else if (isComp(this.lexeme)) {//comparison
                Rvalue rval = new Rvalue("integer");
                if (dtype.equals("integer")) {
                    Integer a = Integer.parseInt(op1.eval(vars).data);
                    Integer b = Integer.parseInt(op2.eval(vars).data);
                    int result = doRel(this.lexeme, a, b);
                    rval.data = String.valueOf(result);
                } else if (dtype.equals("float")) {
                    Double a = NumberUtils.createDouble(op1.eval(vars).data);
                    Double b = NumberUtils.createDouble(op2.eval(vars).data);
                    int result = doRel(this.lexeme, a, b);
                    rval.data = String.valueOf(result);
                } else {
                    throw new RunException("unsupported type " + dtype);
                }
                return new Return(null, rval);
            } else {
                Rvalue rval = new Rvalue("integer");
                if (dtype.equals("integer")) {
                    Integer a = Integer.parseInt(op1.eval(vars).data);
                    Integer b = Integer.parseInt(op2.eval(vars).data);
                    boolean result = this.lexeme.equals("&&") ? (a > 0 && b > 0) : (a > 0 || b > 0);
                    rval.data = String.valueOf(result ? 1 : 0);
                } else if (dtype.equals("float")) {
                    Double a = NumberUtils.createDouble(op1.eval(vars).data);
                    Double b = NumberUtils.createDouble(op2.eval(vars).data);
                    boolean result = this.lexeme.equals("&&") ? (a > 0 && b > 0) : (a > 0 || b > 0);
                    rval.data = String.valueOf(result ? 1 : 0);
                } else {
                    throw new RunException("unsupported type " + dtype);
                }
                return new Return(null, rval);
            }
        } else if (this.type == Type.UNARY_OPERATOR) {
            Return op1 = this.children.get(0).evaluate(vars, fxnlist);
            if (op1.eval(vars) == null) {
                throw new RunException("evaluation of void expression");
            }
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
                String cond; 
                if (this.children.get(1) == null) {
                    cond = "1";
                } else {
                    Return rval = this.children.get(1).evaluate(vars, fxnlist);
                    if (rval.eval(vars) == null) {
                        throw new RunException("evaluation of void expression");
                    } else {
                        cond = rval.eval(vars).data; 
                    }
                }
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
                String res = "";
                for (int i = 0; i < this.children.size(); ++i) {
                    Rvalue ev = this.children.get(i).evaluate(vars, fxnlist).eval(vars);
                    if (ev == null) {
                        throw new RunException("evaluation of void expression");
                    }
                    res += ev.data;
                }
                System.out.print(res);
                return new Return();
            } else {
                //System.out.println("function call " + this.lexeme);
                Node function = fxnlist.get(this.lexeme);
                int N = function.children.size();
                RunInfo.Scope sc = new RunInfo.Scope();

                for (int i = 0; i + 1 < N; ++i) {
                    Rvalue rval = this.children.get(i).evaluate(vars, fxnlist).eval(vars);
                    String name = function.children.get(i).lexeme;
                    sc.variables.put(name, rval);
                }

                //insert scope
                vars.addFunctionScope();
                vars.addScope(sc);

                //execute the function block
                Return r = function.children.get(N - 1).evaluate(vars, fxnlist);
                Return ret = new Return();
                if (r.name != null) {
                    if (!r.name.equals("return")) {
                        throw new RunException("returned was not return " + r.name);
                    }
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
