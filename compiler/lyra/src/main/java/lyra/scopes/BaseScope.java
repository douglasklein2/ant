package lyra.scopes;

import lyra.symbols.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic Scope implementation.
 */
public class BaseScope implements Scope {
    Scope enclosingScope; // null if global (outermost) scope
    Map<String, Symbol> symbols = new LinkedHashMap<>();
    String name = "(anonymous)";

    public BaseScope(Scope parent) { this.enclosingScope = parent;	}

    public Symbol shallowResolve(String name) {
        return symbols.get(name);
    }

    @Override
    public boolean isChildOf(Scope maybeParent) {
        Scope parent = getEnclosingScope();
        while (parent != null) {
            if (parent == maybeParent)
                return true;
            parent = parent.getEnclosingScope();
        }
        return false;
    }

    public Symbol resolve(String name) {
        Symbol s = shallowResolve(name);
        if (s != null )
            return s;
        if ( enclosingScope != null )
            return enclosingScope.resolve(name);
        return null; // not found
    }

    public void define(Symbol sym) {
        symbols.put(sym.getName(), sym);
        sym.setScope(this); // track the scope in each symbol
    }

    public Scope getEnclosingScope() { return enclosingScope; }

    public String toString() { return symbols.keySet().toString(); }

    public String getScopeName() {return name;}
    public void setScopeName(String name) {this.name = name;}
}
