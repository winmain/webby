package goog.ui.ac;

import goog.array.GoogArray;
import goog.string.GoogString;
import goog.ui.ac.Renderer;
import haxe.extern.EitherType;
import js.RegExp;

class RusRenderer extends Renderer {
  /**
 * Transforms a token into a string ready to be put into the regular expression
 * in hiliteMatchingText_.
 * @param {string|Array.<string>} tokenOrArray The token or array to get the
 *     regex string from.
 * @return {string} The regex-ready token.
 * @private
 */
  @:keep
  function getTokenRegExp_(tokenOrArray: EitherType<String, Array<String>>): String {
    var token: String = '';

    if (untyped !tokenOrArray) {
      return token;
    }

    if (Goog.isArray(tokenOrArray)) {
      // Remove invalid tokens from the array, which may leave us with nothing.
      tokenOrArray = GoogArray.filter(untyped tokenOrArray, function(str: String) return !GoogString.isEmptyOrWhitespace(GoogString.makeSafe(str)));
    }

    // If highlighting all tokens, join them with '|' so the regular expression
    // will match on any of them.
    if (highlightAllTokens_) {
      if (Goog.isArray(tokenOrArray)) {
        var tokenArray = GoogArray.map(untyped tokenOrArray, GoogString.regExpEscape);
        token = tokenArray.join('|');
      } else {
        // Remove excess whitespace from the string so bars will separate valid
        // tokens in the regular expression.
        token = GoogString.collapseWhitespace(untyped tokenOrArray);

        token = GoogString.regExpEscape(token);
        token = token.replace(new RegExp(' ', 'g'), '|');
      }
    } else {
      // Not highlighting all matching tokens.  If tokenOrArray is a string, use
      // that as the token.  If it is an array, use the first element in the
      // array.
      // TODO(user): why is this this way?. We should match against all
      // tokens in the array, but only accept the first match.
      if (Goog.isArray(tokenOrArray)) {
        token = untyped tokenOrArray.length > 0 ? GoogString.regExpEscape(tokenOrArray[0]) : '';
      } else {
        // For the single-match string token, we refuse to match anything if
        // the string begins with a non-word character, as matches by definition
        // can only occur at the start of a word. (This also handles the
        // goog.string.isEmptySafe(tokenOrArray) case.)
        if (!new RegExp('^[^0-9a-zA-Zа-яА-Я]').test(tokenOrArray)) {
          token = GoogString.regExpEscape(tokenOrArray);
        }
      }
    }

    return token;
  }

  @:keep
  function hiliteMatchingText_(node: Dynamic, tokenOrArray: Dynamic): Void {
    // patch - в оригинале было так:
    //
    // var re = this.matchWordBoundary_ ?
    //    new RegExp('\\b(?:' + token + ')', 'gi') :
    //    new RegExp(token, 'gi');
    //
    // т.е., с опцией \\b в начале. Я удалил её - это весь патч.
    untyped __js__("
  var node = {0};
  var tokenOrArray = {1};
  if (!this.highlightAllTokens_ && this.wasHighlightedAtLeastOnce_) {
    return;
  }

  if (node.nodeType == goog.dom.NodeType.TEXT) {
    var rest = null;
    if (goog.isArray(tokenOrArray) && tokenOrArray.length > 1 &&
        !this.highlightAllTokens_) {
      rest = goog.array.slice(tokenOrArray, 1);
    }

    var token = this.getTokenRegExp_(tokenOrArray);
    if (token.length == 0) return;

    var text = node.nodeValue;

    // Create a regular expression to match a token at the beginning of a line
    // or preceded by non-alpha-numeric characters. Note: token could have |
    // operators in it, so we need to parenthesise it before adding \\b to it.
    // or preceded by non-alpha-numeric characters
    //
    // NOTE(user): When using word matches, this used to have
    // a (^|\\W+) clause where it now has \\b but it caused various
    // browsers to hang on really long strings. The (^|\\W+) matcher was also
    // unnecessary, because \\b already checks that the character before the
    // is a non-word character, and ^ matches the start of the line or following
    // a line terminator character, which is also \\W. The regexp also used to
    // have a capturing match before the \\b, which would capture the
    // non-highlighted content, but that caused the regexp matching to run much
    // slower than the current version.
    var re = this.matchWordBoundary_ ?
        new RegExp('(?:' + token + ')', 'gi') :
        new RegExp(token, 'gi');
    var textNodes = [];
    var lastIndex = 0;

    // Find all matches
    // Note: text.split(re) has inconsistencies between IE and FF, so
    // manually recreated the logic
    var match = re.exec(text);
    var numMatches = 0;
    while (match) {
      numMatches++;
      textNodes.push(text.substring(lastIndex, match.index));
      textNodes.push(text.substring(match.index, re.lastIndex));
      lastIndex = re.lastIndex;
      match = re.exec(text);
    }
    textNodes.push(text.substring(lastIndex));

    // Replace the tokens with bolded text.  Each pair of textNodes
    // (starting at index idx) includes a node of text before the bolded
    // token, and a node (at idx + 1) consisting of what should be
    // enclosed in bold tags.
    if (textNodes.length > 1) {
      var maxNumToBold = !this.highlightAllTokens_ ? 1 : numMatches;
      for (var i = 0; i < maxNumToBold; i++) {
        var idx = 2 * i;

        node.nodeValue = textNodes[idx];
        var boldTag = this.dom_.createElement(goog.dom.TagName.B);
        boldTag.className = this.highlightedClassName;
        this.dom_.appendChild(
            boldTag, this.dom_.createTextNode(textNodes[idx + 1]));
        boldTag = node.parentNode.insertBefore(boldTag, node.nextSibling);
        node.parentNode.insertBefore(
            this.dom_.createTextNode(''), boldTag.nextSibling);
        node = boldTag.nextSibling;
      }

      // Append the remaining text nodes to the end.
      var remainingTextNodes = goog.array.slice(textNodes, maxNumToBold * 2);
      node.nodeValue = remainingTextNodes.join('');

      this.wasHighlightedAtLeastOnce_ = true;
    } else if (rest) {
      this.hiliteMatchingText_(node, rest);
    }
  } else {
    var child = node.firstChild;
    while (child) {
      var nextChild = child.nextSibling;
      this.hiliteMatchingText_(child, tokenOrArray);
      child = nextChild;
    }
  }
", node, tokenOrArray);
  }
}
