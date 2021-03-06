package lyra.symbols.predefined;

import lyra.SemanticErrorException;
import lyra.scopes.Scope;
import lyra.symbols.ClassSymbol;

public class Int extends AbstractPredefinedSymbol {

    @Override
    public void forward(Scope scope) {
        ClassSymbol c = new ClassSymbol("Int", scope, (ClassSymbol)scope.resolve("Number"));
        c.setBinaryNamePrefix("lyra/runtime");
        try {
            forwardMethod(c, "toString",     "String", false);
            forwardMethod(c, "__inc",        "Int", true);
            forwardMethod(c, "__dec",        "Int", true);
            forwardMethod(c, "__not",        "Int", true);
            forwardMethod(c, "__positive",   "Int", true);
            forwardMethod(c, "__negative",   "Int", true);
            forwardMethod(c, "__multiplied", "Int", true, new ArgumentStrings("Int", "rhs"));
            forwardMethod(c, "__divided",    "Int", true, new ArgumentStrings("Int", "rhs"));
            forwardMethod(c, "__remainder",  "Int", true, new ArgumentStrings("Int", "rhs"));
            forwardMethod(c, "__added",      "Int", true, new ArgumentStrings("Int", "rhs"));
            forwardMethod(c, "__subtracted", "Int", true, new ArgumentStrings("Int", "rhs"));
            forwardMethod(c, "toString",     "String", false);
            defineClass(scope, c);
        } catch (SemanticErrorException e) {
            throw new RuntimeException("Compiler not obeying it's own rules.", e);
        }
    }

}
