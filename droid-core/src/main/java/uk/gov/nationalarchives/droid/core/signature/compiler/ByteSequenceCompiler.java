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

import net.byteseek.compiler.CompileException;
import net.byteseek.compiler.matcher.SequenceMatcherCompiler;
import net.byteseek.matcher.sequence.SequenceMatcher;
import net.byteseek.parser.ParseException;
import net.byteseek.parser.tree.ParseTree;
import net.byteseek.parser.tree.ParseTreeType;
import net.byteseek.parser.tree.node.BaseNode;
import net.byteseek.parser.tree.node.ChildrenNode;

import uk.gov.nationalarchives.droid.core.signature.droid6.ByteSequence;
import uk.gov.nationalarchives.droid.core.signature.droid6.SideFragment;
import uk.gov.nationalarchives.droid.core.signature.droid6.SubSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//TODO: do we need to clear existing data in byte sequences, or just refuse to compile if they already have something?
//TODO: if we use ShiftOR searchers, then ANY bytes can be included in a search without penalty....
//TODO: optimise single byte alternative expressions into a set.


public class ByteSequenceCompiler {

    /**
     * The maximum byte range which can be included in an anchoring sequence, rather than having to be a fragment.
     * A range of bytes can be specified instead of a single byte value.
     * The biggest range is all the bytes, which is 256.
     * This max only applies when compiling for DROID.  When compiling for PRONOM, only a sequence of single
     * byte values are allowed to be part of the anchoring sequence, so the max range is 1!
     *
     * High values can cause performance problems for many search algorithms.
     * Low values impact performance a bit by forcing us to process ranges as fragments.
     * So we want an optimum value that allows some ranges in our search sequences, but not too big a range.
     * The biggest downside happens with large ranges, and the upside of allowing a range isn't huge,
     * so we bias towards low ranges.
     */
    private static final int MAX_RANGE = 64;

    //TODO: produce objects optimised for DROID, or optimised for PRONOM XML output.
    public enum CompileType {
        PRONOM, DROID;
    }

    public static final ByteSequenceCompiler COMPILER = new ByteSequenceCompiler();

    private static final SequenceMatcherCompiler MATCHER_COMPILER = new SequenceMatcherCompiler();
    private static final ParseTree ZERO_TO_MANY = new ChildrenNode(ParseTreeType.ZERO_TO_MANY, BaseNode.ANY_NODE);


    public ByteSequence compileByteSequence(final String droidExpression,
                                            final ByteSequenceAnchor anchor,
                                            final CompileType compileType) throws CompileException {
        final ByteSequence newByteSequence = new ByteSequence();
        newByteSequence.setReference(anchor.getAnchorText());
        compileByteSequence(newByteSequence, droidExpression, compileType);
        return newByteSequence;
    }

    /**
     * Compiles a ByteSequence from a DROID syntax regular expression, and how it is anchored to the BOF or EOF (or is a
     * variable type of anchor).
     *
     * @param sequence The ByteSequence which will be altered by compilation.
     * @param droidExpression The string containing a DROID syntax regular expression.
     * @param anchor How the ByteSequence is to be anchored to the BOF, EOF or a variable search from BOF.
     * @param compileType how to build the objects from the expression.
     * @throws CompileException If there is a problem compiling the DROID regular expression.
     */
    public void compileByteSequence(final ByteSequence sequence,
                                    final String droidExpression,
                                    final ByteSequenceAnchor anchor,
                                    final CompileType compileType) throws CompileException {
        sequence.setReference(anchor.getAnchorText());
        compileByteSequence(sequence, droidExpression, compileType);
    }

    /**
     * Compiles a ByteSequence from a DROID syntax regular expression.
     * <p>
     * It is assumed that the ByteSequence has already defined how it is anchored to the BOF or EOF (or is a variable
     * type of anchor).
     *
     * @param sequence The ByteSequence which will be altered by compilation.
     * @param droidExpression The string containing a DROID syntax regular expression.
     * @param compileType how to build the objects from the expression.
     * @throws CompileException If there is a problem compiling the DROID regular expression.
     */
    public void compileByteSequence(final ByteSequence sequence,
                                    final String droidExpression,
                                    final CompileType compileType) throws CompileException {
        try {
            // Parse the expression into an abstract syntax tree (AST)
            final ParseTree sequenceNodes = ByteSequenceParser.PARSER.parseByteSequence(droidExpression);

            // Compile the ByteSequence from the AST:
            compileByteSequence(sequence, sequenceNodes, compileType);
        } catch (ParseException ex) {
            throw new CompileException(ex.getMessage(), ex);
        }
    }

    /**
     * Compiles a ByteSequence from an abstract syntax tree (AST).
     * <p>
     * 1.  Pre-processes the AST nodes to ensure wildcards are placed into the correct sub-sequence.
     * 2.  Compiles each subsequence in turn and adds it to the ByteSequence.
     *
     * @param sequence      The ByteSequence object to which SubSequences will be added.
     * @param sequenceNodes An Abstract Syntax Tree of a droid expression.
     * @param compileType how to build the objects from the expression.
     * @throws CompileException If there is a problem compiling the AST.
     */
    private void compileByteSequence(final ByteSequence sequence,
                                     final ParseTree sequenceNodes,
                                     final CompileType compileType) throws CompileException {
        final boolean anchoredToEnd = "EOFoffset".equals(sequence.getReference());

        // Pre-process the parse tree, re-ordering some wildcards on subsequence boundaries.
        // Some wildcards at the stopValue or beginning of subsequences belong in the next/prior subsequence object,
        // depending on whether the overall byte sequence is anchored to the beginning or stopValue of a file.
        // It also replaces MIN_TO_MANY nodes with a REPEAT node (MIN) and a ZERO_TO_MANY node (MANY),
        // since these are logically equivalent, but easier to subsequently process once split.
        // The result of pre-processing is that each distinct subsequence is then trivially identified,
        // and contains all the information it needs to be built, without having to refer to information at the
        // stopValue or start of another subsequence.
        final List<ParseTree> sequenceList = preprocessSequence(sequenceNodes, anchoredToEnd, compileType);
        final int numNodes = sequenceList.size();

                // Scan through all the syntax tree nodes, building subsequences as we go (separated by * nodes):
        int subSequenceStart = 0;
        for (int nodeIndex = 0; nodeIndex < numNodes; nodeIndex++) {
            if (sequenceList.get(nodeIndex).getParseTreeType() == ParseTreeType.ZERO_TO_MANY) {
                sequence.addSubSequence(buildSubSequence(sequenceList, subSequenceStart, nodeIndex, anchoredToEnd, compileType));
                subSequenceStart = nodeIndex + 1;
            }
        }

        // Add final SubSequence, if any:
        if (subSequenceStart < numNodes) {
            sequence.addSubSequence(buildSubSequence(sequenceList, subSequenceStart, numNodes, anchoredToEnd, compileType));
        }

        //TODO: any other attributes we need to set here (min offsets or something?).
        //      Min offset of ByteSequence itself?
    }

    private List<ParseTree> preprocessSequence(final ParseTree sequenceNodes, final boolean anchoredToEnd, final CompileType compileType) {
        final int numNodes = sequenceNodes.getNumChildren();
        final IntIterator index = anchoredToEnd ? new IntIterator(numNodes - 1, 0) : new IntIterator(0, numNodes - 1);
        final List<ParseTree> sequenceList = new ArrayList<>();
        int lastValuePosition = -1; // what about first pos?
        while (index.hasNext()) {
            final ParseTree node = sequenceNodes.getChild(index.next());
            sequenceList.add(node);
            switch (node.getParseTreeType()) {
                case BYTE:
                case RANGE:
                case STRING:
                case ANY: {
                    lastValuePosition = sequenceList.size() - 1;
                    break;
                }
                case ALTERNATIVES: {
                    lastValuePosition = sequenceList.size() - 1;
                    if (compileType == CompileType.DROID) {
                        sequenceList.set(sequenceList.size() - 1, optimiseSingleByteAlternatives(node));
                    }
                    break;
                }
                case ZERO_TO_MANY: { // subsequence boundary.
                    sequenceList.add(lastValuePosition + 1, node); // insert zero to many node after last value position.
                    sequenceList.remove(sequenceList.size() - 1);  // remove existing zero to many node on stopValue.
                    break;
                }
                case REPEAT_MIN_TO_MANY: { // subsequence boundary {n,*}
                    sequenceList.set(sequenceList.size() - 1, new ChildrenNode(ParseTreeType.REPEAT, node.getChild(0), node.getChild(1)));
                    sequenceList.add(lastValuePosition + 1, ZERO_TO_MANY);
                    break;
                }
            }
        }
        if (anchoredToEnd) {
            Collections.reverse(sequenceList);
        }
        return sequenceList;
    }

    /**
     * Looks for alternatives which match several different single bytes.
     * These can be more efficiently represented in DROID using a Set matcher from byteseek,
     * rather than a list of SideFragments in DROID itself.
     * <p>
     * @param node The node to optimise
     * @return An optimised list of alternatives, or a single SET matcher node if all the alternatives just match single bytes.
     */
    private ParseTree optimiseSingleByteAlternatives(final ParseTree node) {
        //TODO: write optimisation routines.
        return node;
    }

    private SubSequence buildSubSequence(final List<ParseTree> sequenceList,
                                         final int subSequenceStart, final int subSequenceEnd,
                                         final boolean anchoredToEOF,
                                         final CompileType compileType) throws CompileException {
        /// Find and compile the anchoring search sequence:
        final IntPair anchorRange = locateSearchSequence(sequenceList, subSequenceStart, subSequenceEnd, compileType);
        final ParseTree anchorSequence  = createSubSequenceTree(sequenceList, anchorRange.firstInt, anchorRange.secondInt);
        final SequenceMatcher anchorMatcher = MATCHER_COMPILER.compile(anchorSequence);

        final List<List<SideFragment>> leftFragments = new ArrayList<>();
        final IntPair leftOffsets =
                createLeftFragments(sequenceList, leftFragments,
                        anchorRange.firstInt - 1, subSequenceStart);

        final List<List<SideFragment>> rightFragments = new ArrayList<>();
        final IntPair rightOffsets =
                createRightFragments(sequenceList, rightFragments,
                        anchorRange.secondInt + 1, subSequenceEnd - 1);

        // Choose which remaining fragment offsets are used for the subsequence offsets as a whole:
        final IntPair subSequenceOffsets = anchoredToEOF ? rightOffsets : leftOffsets;

        return new SubSequence(anchorMatcher, leftFragments, rightFragments, subSequenceOffsets.firstInt, subSequenceOffsets.secondInt);
    }

    private IntPair createLeftFragments(final List<ParseTree> sequenceList, final List<List<SideFragment>> leftFragments,
                                        final int fragmentStart, final int fragmentEnd) throws CompileException {
        int position = 1;
        int minGap   = 0;
        int maxGap   = 0;
        int startValueIndex = Integer.MAX_VALUE;
        int endValueIndex = Integer.MAX_VALUE;
        for (int fragmentIndex = fragmentStart; fragmentIndex >= fragmentEnd; fragmentIndex--) {
            final ParseTree node = sequenceList.get(fragmentIndex);
            switch (node.getParseTreeType()) {

                case BYTE:
                case ANY:
                case RANGE:
                case STRING: { // nodes carrying a match for a byte or bytes:
                    // Track when we first encounter the stopValue of a fragment sequence (we hit this first as we go backwards for left fragments):
                    if (endValueIndex == Integer.MAX_VALUE) {
                        endValueIndex = fragmentIndex;
                    }
                    // Continuously track the start of any fragment.
                    startValueIndex = fragmentIndex;
                    break;
                }

                case REPEAT:
                case REPEAT_MIN_TO_MAX: { // wildcard gaps
                    if (startValueIndex == fragmentIndex + 1) { // Add any previous value not yet processed.
                        leftFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
                        position++;
                        minGap = 0;
                        maxGap = 0;
                        startValueIndex = Integer.MAX_VALUE;
                        endValueIndex   = Integer.MAX_VALUE;
                    }
                    minGap += getMinGap(node);
                    maxGap += getMaxGap(node);
                    break;
                }

                case ALTERNATIVES: { // a set of alternatives - these always form fragments of their own.
                    if (startValueIndex == fragmentIndex + 1) { // Add any previous value not yet processed:
                        leftFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
                        position++;
                        minGap = 0;
                        maxGap = 0;
                    }
                    // Add alternatives
                    leftFragments.add(compileAlternatives(node, minGap, maxGap, position));
                    position++;
                    minGap = 0;
                    maxGap = 0;
                    startValueIndex = Integer.MAX_VALUE;
                    endValueIndex   = Integer.MAX_VALUE;
                    break;
                }

                default: throw new CompileException("Unknown node type: " + node + " found at node index: " + fragmentIndex);
            }
        }

        // Add any final unprocessed value fragment at start:
        if (startValueIndex == fragmentEnd) {
            leftFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
            minGap = 0;
            maxGap = 0;
        }

        // Return any final min / max gap left over at the start of the left fragments:
        return new IntPair(minGap, maxGap);
    }

    private IntPair createRightFragments(final List<ParseTree> sequenceList, final List<List<SideFragment>> rightFragments,
                                         final int fragmentStart, final int fragmentEnd) throws CompileException {
        int position = 1;
        int minGap = 0;
        int maxGap = 0;
        int startValueIndex = Integer.MAX_VALUE;
        int endValueIndex = Integer.MAX_VALUE;
        for (int fragmentIndex = fragmentStart; fragmentIndex <= fragmentEnd; fragmentIndex++) {
            final ParseTree node = sequenceList.get(fragmentIndex);
            switch (node.getParseTreeType()) {

                case BYTE:
                case ANY:
                case RANGE:
                case STRING: { // nodes carrying a match for a byte or bytes:
                    // Track when we first encounter the start of a fragment sequence
                    if (startValueIndex == Integer.MAX_VALUE) {
                        startValueIndex = fragmentIndex;
                    }
                    // Continuously track the stopValue of any fragment.
                    endValueIndex = fragmentIndex;
                    break;
                }

                case REPEAT:
                case REPEAT_MIN_TO_MAX: { // wildcard gaps
                    if (endValueIndex == fragmentIndex - 1) { // Add any previous value not yet processed.
                        rightFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
                        position++;
                        minGap = 0;
                        maxGap = 0;
                        startValueIndex = Integer.MAX_VALUE;
                        endValueIndex = Integer.MAX_VALUE;
                    }
                    minGap += getMinGap(node);
                    maxGap += getMaxGap(node);
                    break;
                }

                case ALTERNATIVES: { // a set of alternatives - these always form fragments of their own.
                    if (endValueIndex == fragmentIndex - 1) { // Add any previous value not yet processed:
                        rightFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
                        position++;
                        minGap = 0;
                        maxGap = 0;
                    }
                    // Add alternatives
                    rightFragments.add(compileAlternatives(node, minGap, maxGap, position));
                    position++;
                    minGap = 0;
                    maxGap = 0;
                    startValueIndex = Integer.MAX_VALUE;
                    endValueIndex = Integer.MAX_VALUE;
                    break;
                }

                default:
                    throw new CompileException("Unknown node type: " + node + " found at node index: " + fragmentIndex);
            }
        }

        // Add any final unprocessed value fragment at stopValue:
        if (endValueIndex == fragmentEnd) {
            rightFragments.add(buildFragment(sequenceList, startValueIndex, endValueIndex, minGap, maxGap, position));
            minGap = 0;
            maxGap = 0;
        }

        // Return any final min / max gap left over at the start of the left fragments:
        return new IntPair(minGap, maxGap);
    }

    private List<SideFragment> buildFragment(List<ParseTree> sequenceList, int startValueIndex, int endValueIndex,
                                             int minGap, int maxGap, int position) throws CompileException {
        final List<SideFragment> fragments = new ArrayList<>();
        final ParseTree fragmentTree = createSubSequenceTree(sequenceList, startValueIndex, endValueIndex);
        final SequenceMatcher matcher = MATCHER_COMPILER.compile(fragmentTree);
        final SideFragment fragment = new SideFragment(matcher, minGap, maxGap, position);
        fragments.add(fragment);
        return fragments;
    }

    private ParseTree createSubSequenceTree(List<ParseTree> sequenceList, int subSequenceStart, int subSequenceEnd) {
        // Sublist uses an exclusive end index, so we have to add one to the end position we have to get the right list.
        return new ChildrenNode(ParseTreeType.SEQUENCE, sequenceList.subList(subSequenceStart, subSequenceEnd + 1));
    }

    private List<SideFragment> compileAlternatives(ParseTree node, int minGap, int maxGap, int position) throws CompileException {
        final int numChildren = node.getNumChildren();
        final List<SideFragment> alternatives = new ArrayList<>();
        for (int childIndex = 0; childIndex < numChildren; childIndex++) {
            final ParseTree alternative = node.getChild(childIndex);
            final SequenceMatcher fragmentMatcher = MATCHER_COMPILER.compile(alternative);
            final SideFragment fragment = new SideFragment(fragmentMatcher, minGap, maxGap, position);
            alternatives.add(fragment);
        }
        return alternatives;
    }

    private int getMinGap(final ParseTree node) throws CompileException {
        if (node.getParseTreeType() == ParseTreeType.REPEAT ||
            node.getParseTreeType() == ParseTreeType.REPEAT_MIN_TO_MAX) {
            try {
                return node.getNumChildren() > 0 ? node.getChild(0).getIntValue() : 0;
            } catch (ParseException ex) {
                throw new CompileException(ex.getMessage(), ex);
            }
        }
        return 0;
    }

    private int getMaxGap(final ParseTree node) throws CompileException {
        if (node.getParseTreeType() == ParseTreeType.REPEAT_MIN_TO_MAX) {
            try {
                return node.getNumChildren() > 1 ? node.getChild(1).getIntValue() : 0;
            } catch (ParseException ex) {
                throw new CompileException(ex.getMessage(), ex);
            }
        }
        return 0;
    }

    /**
     *
     * @param sequence
     * @param startIndex
     * @param endIndex   The end index (exclusive) of the last node to search for.
     * @return The start and end indexes of the search sequence as an IntPair.
     * @throws CompileException
     */
    private IntPair locateSearchSequence(final List<ParseTree> sequence,
                                         final int startIndex, final int endIndex,
                                         final CompileType compileType) throws CompileException {
        final int numNodes = sequence.size();
        int length = 0;
        int startPos = startIndex;
        int bestLength = 0;
        int bestStart  = 0;
        int bestEnd    = 0;
        for (int childIndex = 0; childIndex < endIndex; childIndex++) {
            ParseTree child = sequence.get(childIndex);

            switch (child.getParseTreeType()) {

                // Children that match a single byte position:
                case BYTE: {
                    //TODO: if byte is inverted, then should we add it to stopValue/start of anchoring sequence?
                    length++; // add one to the max length found.
                    break;
                }

                // Strings add the length of the string to the byte sequence:
                case STRING: {
                    try {
                        length += child.getTextValue().length(); // Add the string length.
                    } catch (ParseException ex) {
                        throw new CompileException(ex.getMessage(), ex);
                    }
                    break;
                }

                case RANGE: {
                    if (compileType == CompileType.DROID && getRange(child) <= MAX_RANGE) {
                        length++; // treat the range as part of an anchor sequence, not something that has to be a fragment.
                    } else { // something that needs a fragment.
                        // If we found a longer sequence that we had so far, use that:
                        if (length > bestLength) {
                            bestLength = length;
                            bestStart  = startPos;
                            bestEnd    = childIndex - 1; //TODO: check this logic.
                        }

                        // Start looking for a longer suitable sequence:
                        length = 0;
                        startPos = childIndex + 1; // next subsequence to look for.
                    }
                    break;
                }
                // Types which can't form part of an anchoring subsequence, and have to be fragments:
                case ALTERNATIVES: case ANY: case REPEAT: case REPEAT_MIN_TO_MAX: {
                    // If we found a longer sequence that we had so far, use that:
                    if (length > bestLength) {
                        bestLength = length;
                        bestStart  = startPos;
                        bestEnd    = childIndex - 1; //TODO: check this logic.
                    }

                    // Start looking for a longer suitable sequence:
                    length = 0;
                    startPos = childIndex + 1; // next subsequence to look for.
                    break;
                }
            }
        }

        // Do a final check to see if the last nodes processed are the longest:
        if (length > bestLength) {
            bestLength = length;
            bestStart  = startPos;
            bestEnd    = numNodes - 1;
        }

        // If we have no best length, then we have no anchoring subsequence - DROID can't process it.
        if (bestLength == 0) {
            throw new CompileException("No suitable anchoring sequence found."); //TODO: add context to error messages.
        }

        return new IntPair(bestStart, bestEnd);
    }

    // Only include ranges as potential anchor members if they are not too big.
    // Large ranges are poor members for a search anchor, as they can massively impede many search algorithms if they're present.
    private int getRange(final ParseTree rangeNode) throws CompileException {
        if (rangeNode.getParseTreeType() == ParseTreeType.RANGE) {
            final int range1, range2;
            try {
                range1 = rangeNode.getChild(0).getIntValue();
                range2 = rangeNode.getChild(1).getIntValue();
            } catch (ParseException e) {
                throw new CompileException(e.getMessage(), e);
            }
            return range2 > range1 ? range2 - range1 : range1 - range2; // range values are not necessarily smaller to larger...
        }
        throw new IllegalArgumentException("Parse tree node is not a RANGE type: " + rangeNode);
    }

    private static class IntPair {
        public final int firstInt;
        public final int secondInt;
        public IntPair(final int firstInt, final int secondInt) {
            this.firstInt = firstInt;
            this.secondInt = secondInt;
        }
    }

    private static class IntIterator {
        private final int stopValue;
        private final int increment;
        private int position;

        public IntIterator(final int start, final int end) {
            this.position = start;
            this.increment = start < end ? 1 : -1;
            this.stopValue = end + increment;
        }

        public boolean hasNext() {
            return position != stopValue;
        }

        public int next() {
            final int currentPosition = position;
            position += increment;
            return currentPosition;
        }
    }

}