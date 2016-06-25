package parser;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.math.*;
import static java.util.Arrays.asList;

class Token {
    public int type;
    public String lexeme;
}

class Lexer {
    private String data = "";

    public HashSet<String> operators = new HashSet<String>();
    public HashSet<String> alphabets = new HashSet<String>();
    ArrayList<String> lexemes;
    int next_lexeme;

    Lexer() {
        operators.add("+");
        operators.add("-");
        operators.add("*");
        operators.add("/");
        operators.add("=");
        operators.add(",");
        operators.add("<");
        operators.add(">");
        operators.add("&");
        operators.add("|");

        alphabets.addAll(operators);
        alphabets.add("(");
        alphabets.add(")");
        alphabets.add("{");
        alphabets.add("}");
        alphabets.add(";");
        alphabets.add("\n");
        alphabets.add("\"");
    }

    //reset the current lexeme position
    public void reset() {
        this.next_lexeme = 0;
    }

    //undo one move
    public void undo() { 
        this.next_lexeme--;
    }

    public void setData(String data) {
        this.data = data;
        reset();
    }

    private boolean isSpace(String ch) {
        return ch.equals(" ") || ch.equals("\t");
    }
   
    public boolean finished() {
        return this.next_lexeme == this.lexemes.size();
    }

    public String next() {
        return this.lexemes.get(this.next_lexeme++);
    }

    public boolean process() {
        int pos = 0;
        ArrayList<Token> tokens = new ArrayList<Token>();
        this.lexemes = new ArrayList<String>();
        int N = this.data.length();
        while (pos < N) {
            if (!this.isSpace(String.valueOf(data.charAt(pos)))) {
                String tok = "";
                if (data.charAt(pos) == '"') {
                    tok += "\"";
                    boolean ok = false;
                    while (++pos < N) {
                        if (data.charAt(pos) == '\\') {
                            ++pos;
                            if (pos < N) {
                                char ch = data.charAt(pos);
                                if (ch == '\n') break; //newline before string was closed
                                else if (ch == 'n') tok += "\n";
                                else if (ch == '\\') tok += "\\";
                                else if (ch == '"') tok += "\"";
                                else if (ch == 't') tok += "\t";
                                else {
                                    System.out.println("warning: unknown escape sequence removed from input");
                                }
                            } else {
                                System.out.println("warning: ignoring unmatched escape character");
                            }
                        } else if (data.charAt(pos) == '"') { //end of string
                            tok += "\"";
                            ok = true;
                            break;
                        } else if (data.charAt(pos) == '\n') break;
                        else tok += String.valueOf(data.charAt(pos)); 
                    }
                    if (!ok) {
                        System.out.println("error: non terminated string");
                        return false;
                    }
                } else {
                    while (pos < N) {
                        String ch = String.valueOf(this.data.charAt(pos));
                        if (this.isSpace(ch)) break;
                        else if (alphabets.contains(ch)) {
                            if (tok.isEmpty()) {
                                tok = ch; 
                                if (tok.equals("=") || tok.equals("<") || tok.equals(">")) {
                                    if (pos + 1 < N && this.data.charAt(pos + 1) == '=') {
                                        pos++;
                                        tok += "=";
                                    }
                                } else if (tok.equals("&") || tok.equals("|")) {
                                    if (pos + 1 < N) {
                                        if (this.data.charAt(pos + 1) == tok.charAt(0)) {
                                            pos++;
                                            tok += tok;
                                            System.out.println("token " + tok);
                                        }
                                    }
                                }
                            } else pos--;
                            break;
                        }
                        tok += ch;
                        pos++;
                    }
                }
                lexemes.add(tok);
            }
            pos++;
        }
        this.next_lexeme = 0;
        return true;
    }
}
