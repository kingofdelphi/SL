CPATH = ".:commons.jar"
prg:
	javac -cp $(CPATH) parser.java main.java lexer.java
	java -cp $(CPATH) Main
