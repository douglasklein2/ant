package lyra.listeners;

import lyra.LyraLexer;
import lyra.LyraParser;

import lyra.tokens.NumberToken;
import lyra.tokens.StringToken;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import java.net.IDN;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

/**
 *
 */
public class SyntacticSugarListener extends TreeRewriterBaseListener {
    @Override
    public void exitWhilestat(LyraParser.WhilestatContext ctx) {
        ParserRuleContext parent = (ParserRuleContext)ctx.parent;
        LyraParser.ForstatContext replacement = new LyraParser.ForstatContext(parent, -1);
        //'for' varDecl?  ';'  expr ';' expr?  '{' statlist '}';

        replacement.addChild(new CommonToken(LyraLexer.FOR, "for"));
        replacement.addChild(new CommonToken(LyraLexer.SEMICOLON, ";"));

        LyraParser.ExprContext expr = ctx.expr();
        expr.parent = replacement;
        replacement.addChild(expr);

        replacement.addChild(new CommonToken(LyraLexer.SEMICOLON, ";"));
        replacement.addChild(new CommonToken(LyraLexer.LEFTCURLYBRACE, "{"));

        LyraParser.StatlistContext statlist = ctx.statlist();
        statlist.parent = replacement;
        replacement.addChild(statlist);

        replacement.addChild(new CommonToken(LyraLexer.RIGHTCURLYBRACE, "}"));

        replaceChild(ctx, parent, replacement);
    }

    @Override
    public void exitForever(LyraParser.ForeverContext ctx) {
        LyraParser.ForstatContext rewritten = new LyraParser.ForstatContext(ctx.getParent(), -1);

        rewritten.addChild(new CommonToken(LyraLexer.FOR, "for"));
        rewritten.addChild(new CommonToken(LyraLexer.SEMICOLON, ";"));

        LyraParser.ExprContext expr = new LyraParser.ExprContext(rewritten, -1);
        LyraParser.UnaryexprContext uExpr = new LyraParser.UnaryexprContext(expr, -1);
        LyraParser.BoolFactorContext factor = new LyraParser.BoolFactorContext(
                new LyraParser.FactorContext(uExpr, -1));
        factor.addChild(new CommonToken(LyraLexer.TRUE, "true"));
        uExpr.addChild(factor);
        expr.addChild(uExpr);
        rewritten.addChild(expr);

        rewritten.addChild(new CommonToken(LyraLexer.SEMICOLON, ";"));
        rewritten.addChild(new CommonToken(LyraLexer.LEFTCURLYBRACE, "{"));

        LyraParser.StatlistContext statlist = ctx.statlist();
        statlist.parent = rewritten;
        rewritten.addChild(statlist);

        rewritten.addChild(new CommonToken(LyraLexer.RIGHTCURLYBRACE, "}"));

        replaceChild(ctx, ctx.getParent(), rewritten);
    }

    @Override
    public void exitThisMethodFactor(LyraParser.ThisMethodFactorContext ctx) {
        LyraParser.MemberFactorContext rewritten = new LyraParser.MemberFactorContext(new LyraParser.FactorContext(ctx.getParent(), -1));

        LyraParser.NameFactorContext _this = new LyraParser.NameFactorContext(new LyraParser.FactorContext(rewritten, -1));
        _this.addChild(new CommonToken(LyraLexer.IDENT, "this"));
        rewritten.addChild(_this);

        rewritten.addChild(new CommonToken(LyraLexer.DOT, "."));
        rewritten.addChild(new CommonToken(LyraLexer.IDENT, ctx.IDENT().getText()));
        if(ctx.LEFTPARENTHESES() != null){
            rewritten.addChild(new CommonToken(LyraLexer.LEFTPARENTHESES, "("));
            LyraParser.ArgsContext args = ctx.args();
            args.parent = rewritten;
            rewritten.addChild(args);
            rewritten.addChild(new CommonToken(LyraLexer.RIGHTPARENTHESES, ")"));
        }

        replaceChild(ctx, ctx.getParent(), rewritten);
    }

    @Override
    public void exitMethodDecl(LyraParser.MethodDeclContext ctx) {
        if (ctx.type() != null)  return;

        /* default return type is void */
        LyraParser.TypeContext type = new LyraParser.TypeContext(ctx, -1);
        type.addChild(new CommonToken(LyraLexer.IDENT, "void"));
        ctx.addChild(type);
    }

    @Override
    public void exitMethodDeclAbstract(LyraParser.MethodDeclAbstractContext ctx) {
        if (ctx.type() != null) return;

        LyraParser.TypeContext type = new LyraParser.TypeContext(ctx, -1);
        type.addChild(new CommonToken(LyraLexer.IDENT, "void"));
        ctx.addChild(type);
    }

    @Override
    public void exitExpr(LyraParser.ExprContext ctx) {
        if (ctx.unaryexpr() != null)
            return; //handled at exitUnaryexpr
        if (ctx.binOp.getType() == LyraLexer.EQUALOP) {
            return; //handled by ArrayRewriterListener
        }

        /* input has the form rewritten : rewritten opBin rewritten
         * get appropriate method name and rewrite to (rewritten(0)).method(rewritten(1)) */

        ParserRuleContext parent = ctx.getParent();
        LyraParser.ExprContext rewritten = new LyraParser.ExprContext(parent, -1);
        LyraParser.UnaryexprContext uExpr = new LyraParser.UnaryexprContext(rewritten, -1);
        LyraParser.FactorContext factor = new LyraParser.FactorContext(uExpr, -1);
        LyraParser.MemberFactorContext memberFactor
                = new LyraParser.MemberFactorContext(factor);

        memberFactor.addChild(wrapExpressionIntoFactor(memberFactor, ctx.expr(0)));
        memberFactor.addChild(new CommonToken(LyraLexer.DOT, "."));
        memberFactor.addChild(new CommonToken(LyraLexer.IDENT, getBinaryOperatorMethod(ctx.binOp)));
        memberFactor.addChild(new CommonToken(LyraLexer.LEFTPARENTHESES, "("));

        LyraParser.ArgsContext args = new LyraParser.ArgsContext(memberFactor, -1);
        args.addChild(wrapExpression(args, ctx.expr(1)));
        memberFactor.addChild(args);

        memberFactor.addChild(new CommonToken(LyraLexer.RIGHTPARENTHESES, ")"));

        uExpr.addChild(memberFactor);
        rewritten.addChild(uExpr);

        replaceChild(ctx, parent, rewritten);
    }

    @Override
    public void exitUnaryexpr(LyraParser.UnaryexprContext ctx) {
        if (ctx.factor() != null) return;

        String method = null;
        if (ctx.prefixOp != null) {
            method = getPrefixOperatorMethod(ctx.prefixOp);
        } else if (ctx.postfixOp != null) {
            method = getPostfixOperatorMethod(ctx.postfixOp);
        }

        ParserRuleContext parent = ctx.getParent();
        LyraParser.UnaryexprContext rewritten = new LyraParser.UnaryexprContext(parent, -1);
        LyraParser.MemberFactorContext factor = new LyraParser.MemberFactorContext(
                new LyraParser.FactorContext(rewritten, -1));

        /* Due to the parse tree algorithm, our operand is already converted to the
         * "unaryexpr -> factor" form. */
        LyraParser.FactorContext operand = ctx.unaryexpr().factor();
        operand.parent = factor;
        factor.addChild(operand);

        factor.addChild(new CommonToken(LyraLexer.DOT, "."));
        factor.addChild(new CommonToken(LyraLexer.IDENT, method));
        factor.addChild(new CommonToken(LyraLexer.LEFTPARENTHESES, "("));
        /* args rule is required (but it matches epsilon) */
        factor.addChild(new LyraParser.ArgsContext(factor, -1));
        factor.addChild(new CommonToken(LyraLexer.RIGHTPARENTHESES, ")"));

        rewritten.addChild(factor);

        replaceChild(ctx, parent, rewritten);
    }

    LyraParser.ClassdeclContext enumRewritten = null;
    int enumMembers = 0;

    @Override
    public void enterEnumdecl(LyraParser.EnumdeclContext ctx) {
        enumRewritten = new LyraParser.ClassdeclContext(ctx.getParent(), -1);
        enumRewritten.addChild(new CommonToken(LyraLexer.CLASS, "class"));
        enumRewritten.addChild(new CommonToken(LyraLexer.IDENT, ctx.IDENT().getText()));
        enumRewritten.addChild(new CommonToken(LyraLexer.LEFTCURLYBRACE, "{"));
        LyraParser.ClassBodyContext body = new LyraParser.ClassBodyContext(enumRewritten, -1);

        LyraParser.AttributeDeclContext attrib = new LyraParser.AttributeDeclContext(body, -1);
        attrib.addChild(new CommonToken(LyraLexer.VISIBILITYMODIFIER, "public"));

        LyraParser.NameFactorContext null_ = new LyraParser.NameFactorContext(
                new LyraParser.FactorContext(null, -1));
        null_.addChild(new CommonToken(LyraLexer.IDENT, "null"));
        attrib.addChild(createSimpleVarDecl(attrib, "Object", "__value", null_));
        body.addChild(attrib);

        addEnumEquals(body, ctx.IDENT().getText());

        enumMembers = 0;
        enumRewritten.addChild(body);
    }

    @Override
    public void enterUnnamedEnumBody(LyraParser.UnnamedEnumBodyContext ctx) {
        addEnumConstructor(enumRewritten.classBody(), "Int");
    }

    @Override
    public void enterIntEnumBody(LyraParser.IntEnumBodyContext ctx) {
        addEnumConstructor(enumRewritten.classBody(), "Int");
    }

    @Override
    public void enterStringEnumBody(LyraParser.StringEnumBodyContext ctx) {
        addEnumConstructor(enumRewritten.classBody(), "String");
    }

    private void addEnumConstructor(LyraParser.ClassBodyContext body, String typeName) {
        LyraParser.MethodDeclContext ctor = createMethod(body, "public", false, "constructor",
                "void", "value", typeName);
        LyraParser.StatlistContext statlist = new LyraParser.StatlistContext(ctor.methodBody(), -1);
        LyraParser.StatementContext stmt = new LyraParser.StatementContext(statlist, -1);

        LyraParser.ExprContext expr = new LyraParser.ExprContext(stmt, -1);
        expr.addChild(createFieldExpression(expr, "this", "__value"));
        expr.binOp = new CommonToken(LyraLexer.EQUALOP, "=");
        expr.addChild(expr.binOp);
        expr.addChild(createNameFactorExpression(expr, "value"));

        stmt.addChild(expr);
        stmt.addChild(new CommonToken(LyraLexer.SEMICOLON, ";"));
        statlist.addChild(stmt);

        ctor.methodBody().addChild(statlist);
        body.addChild(ctor);
    }

    private void addEnumEquals(LyraParser.ClassBodyContext body, String enumName) {
        LyraParser.MethodDeclContext m = createMethod(body, "public", true, "__equals", "Bool",
                "rhs", enumName);
        LyraParser.MethodBodyContext mBody = m.methodBody();
        LyraParser.StatlistContext statlist = new LyraParser.StatlistContext(mBody, -1);
        LyraParser.StatementContext stmt = new LyraParser.StatementContext(statlist, -1);
        LyraParser.ReturnstatContext ret = new LyraParser.ReturnstatContext(stmt, -1);

        ret.addChild(new CommonToken(LyraLexer.RETURN, "return"));
        ret.addChild(createMethodExpr(ret,
                wrapExpressionIntoFactor(null, createFieldExpression(null, "this", "__value")),
                "__equals", createFieldExpression(null, "rhs", "__value")));

        stmt.addChild(ret);
        stmt.addChild(new CommonToken(LyraLexer.SEMICOLON, ":"));
        statlist.addChild(stmt);
        mBody.addChild(statlist);
        body.addChild(m);
    }

    private void addEnumItem(String name, String typeName, LyraParser.FactorContext factorCtx) {
        LyraParser.ClassBodyContext body = enumRewritten.classBody();

        LyraParser.AttributeDeclContext atrib = new LyraParser.AttributeDeclContext(body, -1);
        atrib.addChild(new CommonToken(LyraLexer.STATIC, "static"));
        atrib.addChild(new CommonToken(LyraLexer.VISIBILITYMODIFIER, "public"));

        atrib.addChild(createSimpleVarDecl(atrib, typeName, name, factorCtx));
        body.addChild(atrib);
    }

    @Override
    public void exitUnamedEnumItem(LyraParser.UnamedEnumItemContext ctx) {
        LyraParser.NumberFactorContext factor = new LyraParser.NumberFactorContext(
                new LyraParser.FactorContext(ctx, -1));
        factor.addChild(new NumberToken(LyraLexer.NUMBER, (new Integer(enumMembers++)).toString()));
        addEnumItem(ctx.IDENT().getText(), "Int", factor);
    }

    @Override
    public void exitStringEnumItem(LyraParser.StringEnumItemContext ctx) {
        String typeName = "String";
        LyraParser.FactorContext factor = new LyraParser.StringFactorContext(new LyraParser.FactorContext(ctx, -1));
        factor.addChild(new StringToken(LyraLexer.STRING, ctx.STRING().getText()));
        addEnumItem(ctx.IDENT().getText(), typeName, factor);
    }

    @Override
    public void exitIntEnumItem(LyraParser.IntEnumItemContext ctx) {
        String typeName = "Int";
        LyraParser.FactorContext factor = new LyraParser.NumberFactorContext(new LyraParser.FactorContext(ctx, -1));
        factor.addChild(new NumberToken(LyraLexer.NUMBER, ctx.NUMBER().getText()));
        addEnumItem(ctx.IDENT().getText(), typeName, factor);
    }

    @Override
    public void exitEnumdecl(LyraParser.EnumdeclContext ctx) {
        enumRewritten.addChild(new CommonToken(LyraLexer.RIGHTCURLYBRACE, "}"));
        replaceChild(ctx, ctx.getParent(), enumRewritten);
    }
}
