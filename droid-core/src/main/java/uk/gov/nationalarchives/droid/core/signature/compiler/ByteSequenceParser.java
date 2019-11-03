/**
 * Copyright (c) 2019, The National Archives <pronom@nationalarchives.gsi.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.core.signature.compiler;

import net.byteseek.parser.ParseException;
import net.byteseek.parser.Parser;
import net.byteseek.parser.StringParseReader;
import net.byteseek.parser.tree.ParseTree;
import net.byteseek.parser.tree.ParseTreeType;
import net.byteseek.parser.tree.node.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class which parses PRONOM and container signature syntax into an abstract syntax tree.
 * This syntax tree can then be analysed and compiled into a ByteSequence and any sub-objects
 * using the {@link ByteSequenceCompiler}.  Any problems during parsing will throw a ParseException.
 * <p>
 * It supports a super-set of the PRONOM syntax, as it also includes any syntax valid for container
 * signatures too.  This means you can also specify strings of ISO-8859-1 characters (Latin-1)
 * delimited by single quotes, and you can use whitespace (space, tab, newline and carriage return) to
 * separate syntactic elements.
 * <p>
 * The resulting AST accurately represents the input string; it does not attempt to validate whether the AST
 * can be further compiled by DROID (e.g. you could just have some wildcards with no byte values).  Whether the AST
 * is compilable is a problem for a compiler (and different compilers may have different attitudes to the same AST).
 * However, it will reject syntactically invalid constructs (e.g. empty alternatives, or unclosed strings).
 * <p>
 * Note that container syntax isn't directly supported by PRONOM, so you would need to remove whitespace
 * and convert strings to hex bytes if you intend to submit a signature as a binary signature to PRONOM.
 */
public final class ByteSequenceParser implements Parser<ParseTree> {

    /*
     * Constants
     */
    private static final char SPACE = ' ';
    private static final char NEWLINE = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char TAB = '\t';
    private static final char QUESTION_MARK = '?';
    private static final char OPEN_SQUARE_BRACKET = '[';
    private static final char OPEN_CURLY_BRACKET = '{';
    private static final char OPEN_ROUND_BRACKET = '(';
    private static final char SINGLE_QUOTE = '\'';
    private static final char CLOSE_ROUND_BRACKET = ')';
    private static final char VERTICAL_BAR = '|';
    private static final char EXCLAMATION_MARK = '!';
    private static final char COLON = ':';
    private static final char CLOSE_SQUARE_BRACKET = ']';
    private static final char CLOSE_CURLY_BRACKET = '}';
    private static final char ASTERISK = '*';
    private static final char HYPHEN = '-';

    private static final ParseTree REPEAT_ANY = new ChildrenNode(ParseTreeType.ZERO_TO_MANY, BaseNode.ANY_NODE);

    /**
     * Convenient static parser (there is no state, so we can just have a static parser).
     */
    public static ByteSequenceParser PARSER = new ByteSequenceParser();

    /**
     * Parses a droid syntax expression into an abstract syntax tree.
     *
     * @param droidExpression A string containing a droid syntax expression.
     * @return A list of abstract syntax trees representing subsequences in the bytesequence.
     * @throws ParseException if there was any problem parsing the expression.
     */
    @Override
    public ParseTree parse(final String droidExpression) throws ParseException {
        final StringParseReader reader = new StringParseReader(droidExpression);
        final List<ParseTree> byteSequenceNodes = new ArrayList<>();
        int currentChar;
        while ((currentChar = reader.read()) >= 0) {
            switch (currentChar) {

                // * Wildcard -
                case ASTERISK: {
                    byteSequenceNodes.add(REPEAT_ANY);
                    break;
                }

                // Whitespace (ignore)
                case SPACE: case NEWLINE: case CARRIAGE_RETURN: case TAB: {
                    break;
                }

                // Hex byte:
                case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': {
                    byteSequenceNodes.add(ByteNode.valueOf(reader.readHexByte(currentChar)));
                    break;
                }

                // Any byte ??
                case QUESTION_MARK: {
                    byteSequenceNodes.add(parseAnyNode(reader));
                    break;
                }

                // Inverted or ranged bytes:
                case OPEN_SQUARE_BRACKET: {
                    byteSequenceNodes.add(parseByteSet(reader));
                    break;
                }

                // Wildcard Gaps {n}, {n-m} and {n-*} - this is complicated by the potential * wildcard we have to process.
                case OPEN_CURLY_BRACKET: {
                    byteSequenceNodes.add(parseGaps(reader));
                    break;
                }

                // Alternative sequences (a|b)
                case OPEN_ROUND_BRACKET: {
                    byteSequenceNodes.add(parseAlternatives(reader));
                    break;
                }

                // Open string (used in container syntax, so support here):
                case SINGLE_QUOTE: {
                    byteSequenceNodes.add(parseString(reader));
                    break;
                }

                default: throw createParseException("Invalid character in droid expression", reader);
            }
        }

        return new ChildrenNode(ParseTreeType.SEQUENCE, byteSequenceNodes);
    }

    private ParseTree parseGaps(final StringParseReader reader) throws ParseException {
        final int firstGapNumber = reader.readInt();
        int nextChar = reader.read();
        switch (nextChar) {
            case CLOSE_CURLY_BRACKET: { // of form {m}
                return new ChildrenNode(ParseTreeType.REPEAT, new IntNode(firstGapNumber), BaseNode.ANY_NODE);
            }
            case HYPHEN: {  // Either {m,n} or {m,*}
                if (reader.peekAhead() == ASTERISK) { // Of form {m,*}
                    reader.read(); // consume the *
                    if (reader.read() == CLOSE_CURLY_BRACKET) { // And is closed by a }
                        return new ChildrenNode(ParseTreeType.REPEAT_MIN_TO_MANY, new IntNode(firstGapNumber));
                    }
                } else { // Of form {m,n}
                    final int secondGapNumber = reader.readInt();
                    if (reader.read() == CLOSE_CURLY_BRACKET) {
                        return new ChildrenNode(ParseTreeType.REPEAT_MIN_TO_MAX,
                                                new IntNode(firstGapNumber), new IntNode(secondGapNumber),
                                                BaseNode.ANY_NODE);
                    }
                }
            }
        }
        throw createParseException("Invalid {n-m} syntax in", reader);
    }

    private ParseTree parseAlternatives(final StringParseReader reader) throws ParseException {
        List<ParseTree> alternatives = new ArrayList<ParseTree>();
        List<ParseTree> sequence = new ArrayList<ParseTree>();

        int currentChar;
        ALTERNATIVES: while ((currentChar = reader.read()) >= 0) {

            switch (currentChar) {

                // Whitespace (ignore)
                case SPACE: case NEWLINE: case CARRIAGE_RETURN: case TAB: {
                    break;
                }

                // Closes alternatives - stop processing.
                case CLOSE_ROUND_BRACKET: {
                    if (sequence.size() == 0) { // no sequence defined before closing:
                        throw createParseException("No sequence defined before closing bracket )", reader);
                    }
                    alternatives.add(createAlternativeNode(sequence));
                    break ALTERNATIVES;
                }

                // Starts a new alternative
                case VERTICAL_BAR: {
                    if (sequence.size() == 0) { // no sequence defined before alternative:
                        throw createParseException("No sequence defined before alternative |", reader);
                    }
                    alternatives.add(createAlternativeNode(sequence));
                    sequence = new ArrayList<ParseTree>(); // start a new sequence list.
                    break;
                }

                // String type: supported in container signatures, so support it here.
                case SINGLE_QUOTE: {
                    sequence.add(parseString(reader));
                    break;
                }

                // Byte set type.
                case OPEN_SQUARE_BRACKET: {
                    sequence.add(parseByteSet(reader));
                    break;
                }

                // Must be a hex byte - add it to the alternative sequence.  Will throw an error if not.
                default: {
                    sequence.add(ByteNode.valueOf(reader.readHexByte(currentChar)));
                }
            }
        }

        // If we've closed the alternatives properly and we have some, return them:
        if (currentChar == ')') {
            if (alternatives.size() == 1) {
                return alternatives.get(0); // no need for alternatives if there is only one alternative.
            } else if (alternatives.size() > 1) {
                return new ChildrenNode(ParseTreeType.ALTERNATIVES, alternatives);
            }
        }
        throw createParseException("Alternatives (a|b) syntax incorrect", reader);
    }

    private ParseTree createAlternativeNode(final List<ParseTree> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        return new ChildrenNode(ParseTreeType.SEQUENCE, values);
    }

    private ParseTree parseByteSet(final StringParseReader reader) throws ParseException {

        // Check whether values are inverted using ! after the open [
        boolean inverted = false;
        if (reader.peekAhead() == EXCLAMATION_MARK) {
            inverted = true;
            reader.read(); // consume the ! character.
        }

        // Must be a hex byte following the open [ or [!
        byte firstByte = reader.readHexByte();

        // Get the next character:
        int nextChar = reader.read();

        // If we're closing the set now, it's a byte or inverted byte:
        if (nextChar == CLOSE_SQUARE_BRACKET) {
            return ByteNode.valueOf(firstByte, inverted);
        }

        // If we're not closing the set now, it must be a range of bytes:
        if (nextChar == COLON) {
            byte secondByte = reader.readHexByte();

            // The next character must close the range after the second byte value:
            if (reader.read() == ']') {
                return new ChildrenNode(ParseTreeType.RANGE, inverted, ByteNode.valueOf(firstByte), ByteNode.valueOf(secondByte));
            }
        }

        throw createParseException("[] syntax incorrect", reader);
    }

    private ParseTree parseString(final StringParseReader reader) throws ParseException {
        return new StringNode(reader.readString('\''));
    }

    private ParseTree parseAnyNode(final StringParseReader reader) throws ParseException {
        if (reader.read() == QUESTION_MARK) {
            return BaseNode.ANY_NODE;
        }
        // must be two ?? together.  Invalid syntax if there isn't a second ?
        throw createParseException("? not followed by another ?", reader);
    }

    private ParseException createParseException(String message, StringParseReader reader) throws ParseException {
        return new ParseException(message + ". Position: " + reader.getPosition() + " in: " + reader.getString());
    }

}