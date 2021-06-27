/*
 * Copyright (c) 2016, The National Archives <pronom@nationalarchives.gov.uk>
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
package uk.gov.nationalarchives.droid.signature;

import java.io.IOException;
import java.util.List;

import uk.gov.nationalarchives.droid.core.interfaces.DroidCore;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResult;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;

/**
 * A class holding matching strategies that implement the MatchStrategy2 interface.
 */
public final class MatchStrategies {

    /**
     * Returns an empty collection of results and does no matching.
     * Useful to baseline the other match strategies against.
     * Note you can never process archives with this strategy, as the archival types will not be recognised.
     */
    public static class NoMatching implements MatchStrategy {

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) {
            IdentificationResultCollection result = new IdentificationResultCollection(request);
            result.setRequestMetaData(request.getRequestMetaData());
            result.setFileLength(request.size());
            return result;
        }
    }

    /**
     * An abstract base class for MatchStrategies2 that use DroidCore functionality.
     * It provides a utility method to add in extension matches after any other matching has been done,
     * as this has proved to be a common requirement for many of them.
     */
    public abstract static class DroidCoreMatchStrategy implements MatchStrategy {

        /**
         * The DroidCore to use with the MatchStrategy.
         */
        private final DroidCore core;

        /**
         * Constructs the base match strategy with a DroidCore.
         *
         * @param core The DroidCore to use.
         */
        public DroidCoreMatchStrategy(DroidCore core) {
            this.core = core;
        }

        /**
         * @return The DroidCore to use when matching.
         */
        protected DroidCore getCore() {
            return core;
        }

        /**
         * Matches extensions only if there aren't better results.
         * If there are better results, it checks for extension mismatches.
         *
         * @param request The IdentificationRequest we are matching.
         * @param results The existing set of results from prior matches.
         * @return An IdentificationResultCollection with extension matching data added.
         */
        protected IdentificationResultCollection addExtensionMatches(IdentificationRequest<?> request,
                                                                     IdentificationResultCollection results) {
            List<IdentificationResult> resultList = results.getResults();
            // If we have no results at all so far, match only on extensions:
            if (resultList != null && resultList.isEmpty()) {
                IdentificationResultCollection extensionResults = core.matchExtensions(request);
                if (extensionResults != null) {
                    return extensionResults;
                }
            } else { // If we have more accurate results than extensions, we won't add in extension matches, as that is just noise.
                // check for extensions mismatches in the results we have.
                core.checkForExtensionsMismatches(results, request.getExtension());
            }
            return results;
        }
    }

    /**
     * Matches only on extensions.
     * If you want to process archives, you should set matchAllExtensions to true, as otherwise the archival
     * types will not be recognised (as there are binary signatures for those extensions, so extensions will not
     * be matched for them if matchAllExtensions is false).
     */
    public static class ExtensionMatching extends DroidCoreMatchStrategy {

        /**
         * Construct an ExtensionMatching strategy.
         * @param core the DroidCore to use.
         */
        public ExtensionMatching(DroidCore core) {
            super(core);
        }

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) {
            return getCore().matchExtensions(request);
        }
    }

    /**
     * Matches binary signatures and extensions.
     * This is how DROID matched before container signatures were introduced.
     */
    public static class BinaryMatching extends DroidCoreMatchStrategy {

        /**
         * Construct a BinaryMatching strategy.
         * @param core the DroidCore to use.
         */
        public BinaryMatching(DroidCore core) {
            super(core);
        }

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) {
            final DroidCore core = getCore();
            IdentificationResultCollection results = core.matchBinarySignatures(request);
            core.removeLowerPriorityHits(results);
            return addExtensionMatches(request, results);
        }
    }

    /**
     * Matches container signatures and extensions.
     * Binary signatures are not run (except the ones which identify ZIP or OLE2 container types).
     * This isn't a particularly useful strategy, except to benchmark the performance impact of different strategies.
     */
    public static class ContainerMatching extends DroidCoreMatchStrategy {

        /**
         * Construct a ContainerMatching strategy.
         * @param core the DroidCore to use.
         */
        public ContainerMatching(DroidCore core) {
            super(core);
        }

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) throws IOException {
            final DroidCore core = getCore();
            IdentificationResultCollection results = core.matchContainerSignatures(request);
            core.removeLowerPriorityHits(results);
            return addExtensionMatches(request, results);
        }
    }

    /**
     * Matches binary first, then any container signatures (if we found any binary signatures), then extensions.
     * If there were no binary matches, we can't have any containers (as we won't have recognised ZIP or OLE2 types).
     * If there was a result from container matching, that is used, otherwise it uses the binary matches.
     * This is the logical strategy used in all earlier versions of DROID that match container signatures.
     */
    public static class BinaryAndContainerMatching extends DroidCoreMatchStrategy {

        /**
         * Construct a BinaryAndContainerMatching strategy.
         * @param core the DroidCore to use.
         */
        public BinaryAndContainerMatching(DroidCore core) {
            super(core);
        }

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) throws IOException {
            final DroidCore core = getCore();
            IdentificationResultCollection results = core.matchBinarySignatures(request);
            if (!results.isEmpty()) { // If we match nothing, then we can't have found the container type.
                IdentificationResultCollection containerResults = core.matchContainerSignatures(request);
                if (!containerResults.isEmpty()) {
                    results = containerResults; // If we found a container match, we discard any binary matches and use the container.
                }
            }
            core.removeLowerPriorityHits(results);
            return addExtensionMatches(request, results);
        }
    }

    /**
     * Matches container first (using binary matching for container types only, e.g. OLE2 or ZIP),
     * but only matches all the other binary signatures if there were no results from container matching,
     * then adds any extensions match data.  This should be more efficient than the traditional strategy, since it
     * avoids running the majority of binary signatures if we already found a container.  It gives identical
     * results to the BinaryAndContainerMatching strategy (since a container match is always returned over any binary
     * matches).
     */
    public static class ContainerOrBinaryMatching extends DroidCoreMatchStrategy {

        /**
         * Construct a ContainerOrBinaryMatching strategy.
         * @param core the DroidCore to use.
         */
        public ContainerOrBinaryMatching(DroidCore core) {
            super(core);
        }

        @Override
        public IdentificationResultCollection match(IdentificationRequest request) throws IOException {
            final DroidCore core = getCore();
            IdentificationResultCollection results = core.matchContainerSignatures(request);
            if (results.isEmpty()) {
                results = core.matchBinarySignatures(request);
            }
            core.removeLowerPriorityHits(results);
            return addExtensionMatches(request, results);
        }
    }

}
