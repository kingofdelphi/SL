package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;

//later: make it return references too
class Return {

    public Rvalue value = null;
    public String name = null;

    Return(String variable) {
        this.name = variable;
        this.value = null;
    }

    Return(String variable, Rvalue value) {
        this.name = variable;
        this.value = value;
    }

    Return() {
        this.name = null;
        this.value = null;
    }

    Rvalue eval(RunInfo r) {
        if (name == null && value == null) return null; //void return type
        if (name == null) return value; //rvalue
        return r.getValue(name); //lvalue
    }

}

class Rvalue {
    public String type;
    public String data;
    Rvalue(String type) {
        this.type = type;
    }
}

class RunInfo {

    static class Scope {
        public HashMap<String, Rvalue> variables;
        Scope() {
            variables = new HashMap<String, Rvalue>();
        }
    }

    public ArrayList<ArrayList<Scope>> callstack;
    public ArrayList<Scope> global;

    RunInfo() {
        callstack = new ArrayList<ArrayList<Scope>>();
        global = new ArrayList<Scope>();
    }

    //returns the scope where variable was defined from
    //a stack of scope
    Scope getScopeFromStack(ArrayList<Scope> sc, String var) { 
        for (int i = sc.size() - 1; i >= 0; --i) {
            if (sc.get(i).variables.containsKey(var)) {
                return sc.get(i);
            }
        }
        return null;
    }

    //returns the scope of variable 
    //if variable was not defined, returns null
    //
    Scope getScope(String var) {
        Scope sc = null;

        if (callstack.size() > 0) {
            sc = getScopeFromStack(callstack.get(callstack.size() - 1), var);
            if (sc != null) {
                return sc;
            }
        }

        return getScopeFromStack(global, var);
    }

    Rvalue getValue(String var) {
        Scope sc = getScope(var);
        if (sc != null) return sc.variables.get(var);
        System.out.println("undefined variable " + var);
        return null;
    }

    boolean variableExists(String var) {
        return getScope(var) != null;
    }
   
    void createVariable(String var) {
        //variable doesnot exist so create a new one
        Rvalue value = null;
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            //sl.add(new Scope());
            sl.get(sl.size() - 1).variables.put(var, value);
        } else {
            //System.out.println("created variable " + var);
            global.get(global.size() - 1).variables.put(var, value);
        }
    }

    //override value of pre-existing variable
    void setVariable(String var, Rvalue value) {
        Scope sc = getScope(var);
        if (sc != null) {
            sc.variables.put(var, value);
        } else {
            System.out.println("error, undefined variable assigned");
        }
    }

    void addScope(Scope sc) {
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            sl.add(sc);
        } else {
            global.add(sc);
            //System.out.println("global stck " + global.size());
        }
    }

    void popScope() {
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            sl.remove(sl.size() - 1);
        } else {
            global.remove(global.size() - 1);
            //System.out.println("global stck " + global.size());
        }
    }

    ArrayList<Scope> getCurrentCall() {
        return 
            callstack.get(callstack.size() - 1);
    }

    void addFunctionScope() {
        callstack.add(new ArrayList<Scope>());
    }

    void popFunctionScope() {
        callstack.remove(callstack.size() - 1);
    }

}
