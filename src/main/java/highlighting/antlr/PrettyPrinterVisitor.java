package highlighting.antlr;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/// MiniJava Pretty Printer (minimal, stateful)
///
/// Requirements:
/// - Reproduce the whole program (comments and whitespaces are gone).
/// - Ignore whitespace from the input; instead, generate:
///     - indentation for class bodies and blocks,
///     - exactly one line per statement (lines ending in ';').
///
/// Simplification:
/// Everything that is not indentation or line breaks is printed as raw tokens (with a very simple
/// space heuristic). Expression and signature formatting is therefore not "nice", which is
/// acceptable for this exercise.
public final class PrettyPrinterVisitor extends MiniJavaBaseVisitor<Void> {

  private final StringBuilder out = new StringBuilder();
  private final int indentWidth;
  private int currentIndent = 0;
  private boolean atLineStart = true;

  // For simple spacing between tokens:
  private Token lastToken = null;

  public PrettyPrinterVisitor(int indentWidth) {
    this.indentWidth = Math.max(0, indentWidth);
  }

  public String result() {
    return out.toString();
  }

  // ----------------------------------------------------
  // Structural methods – these enforce indentation and "one statement per line"
  //
  // TODO: implement the four structural visitXyz-methods below: visitCompilationUnit,
  // visitClassBody, visitBlock, and visitStatement
  // ----------------------------------------------------

  @Override
  public Void visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
    if (ctx.packageDecl() != null) {
      visitChildren(ctx.packageDecl());
      nl();
    }
    for (var imp : ctx.importDecl()) {
      visitChildren(imp);
      nl();
    }
    if (!ctx.importDecl().isEmpty() && !ctx.typeDecl().isEmpty()) {
      nl();
    }
    for (var type : ctx.typeDecl()) {
      visit(type);
      nl();
    }
    return null;
  }

  @Override
  public Void visitClassBody(MiniJavaParser.ClassBodyContext ctx) {
    writeln(" {");
    currentIndent++;
    for (var decl : ctx.classBodyDeclaration()) {
      if (decl.memberDecl() != null) {
        indent();
        visitChildren(decl.memberDecl());
        nl();
      }
    }
    currentIndent--;
    write("}");
    return null;
  }

  @Override
  public Void visitBlock(MiniJavaParser.BlockContext ctx) {
    writeln("{");
    currentIndent++;
    for (var stmt : ctx.blockStatement()) {
      if (stmt.localVarDecl() != null) {
        indent();
        visitChildren(stmt.localVarDecl());
        nl();
      } else if (stmt.statement() != null) {
        visit(stmt.statement());
      }
    }
    currentIndent--;
    indent();
    write("}");
    return null;
  }

  @Override
  public Void visitStatement(MiniJavaParser.StatementContext ctx) {
    String first = ctx.getStart().getText();
    if (first.equals("{")) {
      // block statement
      indent();
      visit(ctx.block());
      nl();
    } else if (first.equals("if")) {
      indent();
      write("if (");
      visit(ctx.expression());
      write(") ");
      visitBlockOrStatement(ctx.statement(0));
      if (ctx.statement().size() > 1) {
        write(" else ");
        visitBlockOrStatement(ctx.statement(1));
      }
      nl();
    } else if (first.equals("while")) {
      indent();
      write("while (");
      visit(ctx.expression());
      write(") ");
      visitBlockOrStatement(ctx.statement(0));
      nl();
    } else {
      // return-statement or expression-statement (ends with ';')
      indent();
      visitChildren(ctx);
      nl();
    }
    return null;
  }

  private void visitBlockOrStatement(MiniJavaParser.StatementContext ctx) {
    if (ctx.block() != null) {
      visit(ctx.block());
    } else {
      nl();
      currentIndent++;
      visit(ctx);
      currentIndent--;
    }
  }

  // ---------------- helper methods ----------------

  private void indent() {
    if (atLineStart) {
      out.repeat(" ", Math.max(0, indentWidth * currentIndent));
      atLineStart = false;
    }
  }

  private void write(String s) {
    if (s == null || s.isEmpty()) return;
    indent();
    out.append(s);
  }

  private void nl() {
    out.append('\n');
    atLineStart = true;
    lastToken = null; // Reset spacing context at the beginning of a line
  }

  private void writeln(String s) {
    write(s);
    nl();
  }

  // --------------- token output + basic spacing ---------------

  @Override
  public Void visitTerminal(TerminalNode node) {
    Token t = node.getSymbol();
    String text = t.getText();

    if (lastToken != null) {
      int prevType = lastToken.getType();
      int curType = t.getType();

      // Simple heuristic: insert a space between "word-like" tokens
      if (needsSpaceBetween(prevType, curType)) write(" ");
    }

    write(text);
    lastToken = t;
    return null;
  }

  private boolean needsSpaceBetween(int prevType, int curType) {
    return isWordLike(prevType) && isWordLike(curType);
  }

  private boolean isWordLike(int type) {
    return type == MiniJavaLexer.IDENTIFIER
        || type == MiniJavaLexer.STRING_LITERAL
        || type == MiniJavaLexer.CHAR_LITERAL
        || type == MiniJavaLexer.NULL
        || type == MiniJavaLexer.PACKAGE
        || type == MiniJavaLexer.IMPORT
        || type == MiniJavaLexer.CLASS
        || type == MiniJavaLexer.PUBLIC
        || type == MiniJavaLexer.PRIVATE
        || type == MiniJavaLexer.FINAL
        || type == MiniJavaLexer.RETURN
        || type == MiniJavaLexer.NEW
        || type == MiniJavaLexer.IF
        || type == MiniJavaLexer.ELSE
        || type == MiniJavaLexer.WHILE
        || type == MiniJavaLexer.EXTENDS
        || type == MiniJavaLexer.IMPLEMENTS;
  }
}
