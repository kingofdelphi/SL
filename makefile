CPATH = ".:commons.jar"
prg:
	javac -cp $(CPATH) parser.java main.java lexer.java

run:
	java -cp $(CPATH) Main

clean:
	rm *.class -rf
