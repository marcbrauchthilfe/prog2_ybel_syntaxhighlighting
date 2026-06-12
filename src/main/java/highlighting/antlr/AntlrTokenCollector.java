package highlighting.antlr;

import highlighting.core.HighlightRegion;
import highlighting.core.SyntaxHighlighter;
import highlighting.presets.MiniJavaColours;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.*;

public class AntlrTokenCollector extends SyntaxHighlighter {

  @Override
  public List<HighlightRegion> collectMatches(String text) {
    var lexer = new MiniJavaLexer(CharStreams.fromString(text));
    // Highlighting muss robust gegen ungültigen Input sein: ANTLRs Default-Listener
    // würde "token recognition error ..." auf stderr ausgeben (z.B. bei mehrzeichigen
    // Char-Literalen wie 'someText'). Für reines Highlighting ignorieren wir das.
    lexer.removeErrorListeners();
    var stream = new CommonTokenStream(lexer);
    stream.fill();

    List<HighlightRegion> regions = new ArrayList<>();
    List<Token> tokens = stream.getTokens();

    for (int i = 0; i < tokens.size(); i++) {
      Token t = tokens.get(i);
      if (t.getType() == Token.EOF) continue;

      int start = t.getStartIndex();
      int end = t.getStopIndex() + 1;
      Color colour = colourFor(t.getType());

      if (colour != null) {
        regions.add(new HighlightRegion(start, end, colour));
      }

      if (t.getType() == MiniJavaLexer.AT && i + 1 < tokens.size()) {
        Token next = tokens.get(i + 1);
        if (next.getType() == MiniJavaLexer.IDENTIFIER) {
          regions.add(
              new HighlightRegion(
                  start, next.getStopIndex() + 1, MiniJavaColours.ANNOTATION_COLOUR));
          i++;
        }
      }
    }
    return regions;
  }

  private Color colourFor(int type) {
    return switch (type) {
      case MiniJavaLexer.PACKAGE,
          MiniJavaLexer.IMPORT,
          MiniJavaLexer.CLASS,
          MiniJavaLexer.PUBLIC,
          MiniJavaLexer.PRIVATE,
          MiniJavaLexer.FINAL,
          MiniJavaLexer.RETURN,
          MiniJavaLexer.NULL,
          MiniJavaLexer.NEW,
          MiniJavaLexer.IF,
          MiniJavaLexer.ELSE,
          MiniJavaLexer.WHILE,
          MiniJavaLexer.EXTENDS,
          MiniJavaLexer.IMPLEMENTS ->
          MiniJavaColours.KEYWORD_COLOUR;
      case MiniJavaLexer.STRING_LITERAL -> MiniJavaColours.STRING_LITERAL_COLOUR;
      case MiniJavaLexer.CHAR_LITERAL -> MiniJavaColours.CHAR_LITERAL_COLOUR;
      case MiniJavaLexer.LINE_COMMENT -> MiniJavaColours.LINE_COMMENT_COLOUR;
      case MiniJavaLexer.JAVADOC_COMMENT -> MiniJavaColours.JAVADOC_COMMENT_COLOUR;
      case MiniJavaLexer.BLOCK_COMMENT -> MiniJavaColours.BLOCK_COMMENT_COLOUR;
      default -> null;
    };
  }
}
