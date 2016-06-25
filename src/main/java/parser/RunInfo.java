package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;


class RunInfo {
    //Node represents a node in syntax tree
    static class Scope {
        public HashMap<String, String> variables;
        Scope() {
            variables = new HashMap<String, String>();
        }
    }

    public ArrayList<ArrayList<Scope>> callstack;
    public ArrayList<Scope> global;

    RunInfo() {
        callstack = new ArrayList<ArrayList<Scope>>();
        global = new ArrayList<Scope>();
        global.add(new Scope());
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

    String getValue(String var) {
        Scope sc = getScope(var);
        if (sc != null) return sc.variables.get(var);
        return null;
    }

    //set variable to a certain value
    //if variable does not exist, it means create a variable
    //else override the existing value
    void setVariable(String var, String value) {
        Scope sc = getScope(var);
        if (sc != null) {
            sc.variables.put(var, value);
            return; 
        }

        //variable doesnot exist so create a new one
        //System.out.println("variable new " + var);
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            sl.add(new Scope());
            sl.get(sl.size() - 1).variables.put(var, value);
        } else {
            global.get(global.size() - 1).variables.put(var, value);
        }
    }

    void addScope(Scope sc) {
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            sl.add(sc);
        } else global.add(sc);
    }

    void popScope() {
        if (callstack.size() > 0) {
            ArrayList<Scope> sl = callstack.get(callstack.size() - 1);
            sl.remove(sl.size() - 1);
        } else global.remove(global.size() - 1);
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
