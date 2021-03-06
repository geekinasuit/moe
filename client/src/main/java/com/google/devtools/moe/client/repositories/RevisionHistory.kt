/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.moe.client.repositories

/**
 * An abstraction of a Repository's Revision History.
 */
interface RevisionHistory {
  /**
   * Finds the highest revision less than or equal to revId.
   *
   * @param revId the id of the revision to start at. Null for head.
   * @return the highest Revision less than or equal to revId.
   */
  fun findHighestRevision(revId: String?): Revision?

  /**
   * Reads the metadata for a given revision in the same repository.
   *
   * @param revision the revision to parse metadata for.
   * @return a RevisionMetadata containing metadata for revision
   */
  fun getMetadata(revision: Revision): RevisionMetadata?

  /**
   * Starting at the specified revision, searches the revision history backwards, stopping at
   * matching Revisions. [RevisionMatcher.matches] is called on the given
   * RevisionMatcher for the starting Revisions, then their parents, then the parents' parents, and
   * so on. Then, [RevisionMatcher.makeResult] is called with
   * the non-matching (in breadth-first, child-to-parent order) and matching Revisions, and the
   * result is returned.
   *
   *
   * For example, say you have a RevisionMatcher that matches on revision IDs starting with
   * "match", and you search this history, starting at the top revision:
   * <pre>
   * nonmatch_4
   * /         \
   * nonmatch_3a  nonmatch_3b
   * \         /    \
   * nonmatch_2a  match_2b
   * \           /   \
   * match_1      ...
   * |
   * ...
  </pre> *
   *
   *
   * History is traversed in breadth-first order starting at `nonmatch_4`, and doesn't
   * proceed past `match_1` or `match_2b`. Finally,
   * [RevisionMatcher.makeResult] is called with a list of
   * the non-matching Revisions (nonmatch_4, nonmatch_3a, nonmatch_3b, and nonmatch_2a in that
   * breadth-first order) and a List of the matching Revisions in the order encountered (in this
   * case, match_2b and match_1). This method returns the (arbitrary) result of that call.
   *
   * @param revision the Revision to start at. If null, then start at head revision(s).
   * @param matcher the RevisionMatcher to apply
   * @param searchType the type of history search to perform
   * @return the result of the search, as specified by the type of RevisionMatcher
   */
  // TODO(user): allow specifying multiple Revisions (for case of multiple heads)
  fun <T> findRevisions(
    revision: Revision? = null,
    matcher: RevisionMatcher<T>,
    searchType: SearchType
  ): T

  /**
   * The type of history search to perform in
   * [RevisionHistory.findRevisions].
   */
  enum class SearchType {
    /** Search backward only through a revision's first parent.  */
    LINEAR,

    /** Search backward through each of a revision's parents.  */
    BRANCHED
  }

  companion object {
    const val MAX_REVISIONS_TO_SEARCH = 10000
  }
}
