import java.util.*;
import java.io.*;

import static java.util.Arrays.asList;
//rules
//expression => T [ (+ | -) T]*
//T => F [ (* | /) F]*
//F => identifier | constant

class Parser {
    private Lexer lexer;

    private boolean factor() {
        if (this.lexer.finished()) return false;
        String factor = this.lexer.next();
        if (factor.equals("(")) {
            boolean r =  expression();
            if (r == false) return false;
            if (this.lexer.finished() || !this.lexer.next().equals(")")) {
                System.out.println("missing )");
                return false;
            }
            return true;
        } else if (this.lexer.alphabets.contains(factor)) {
            System.out.println("expected identifier or constant but obtained " + factor);
            return false;
        }
        return true;
    }

    private boolean term() {
        if (!factor()) return false;
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            //lower precedence operator signals end of term
            if (next.equals("+") || next.equals("-") || next.equals(")")) {
                this.lexer.undo();
                return true;
            }
            if (!next.equals("*") && !next.equals("/")) {
                System.out.println("error unexpected" + next + "|");
                return false;
            }
            if (!factor()) return false;
        }
        return true;
    }

    //expression is a sequence of terms added or subtracted
    private boolean expression() {
        if (!term()) {
            System.out.println("invalid expression");
            return false;
        }
        while (!this.lexer.finished()) {
            String next = this.lexer.next();
            if (next.equals(")")) { //the expression was inside brackets
                this.lexer.undo();
                break;
            }
            if (!next.equals("+") && !next.equals("-")) {
                System.out.println("error unexpected |" + next + "|");
                return false;
            }
            if (!term()) return false;
        }
        return true;
    }

    public void main() {
        this.lexer = new Lexer();
        this.lexer.setData("b*(axcz) + (b-(c+d)) - cdefg");
        this.lexer.process();
        while (!this.lexer.finished()) {
            System.out.println(this.lexer.next());
        }
        this.lexer.reset();
        boolean ok = this.expression();
        if (ok) {
            System.out.println("Parse successful\n");
        } else {
            System.out.println("Parse failed\n");
        }
    }

}

