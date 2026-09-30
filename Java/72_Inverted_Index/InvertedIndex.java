import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 72 - An inverted index: tokenise documents, search terms, combine results with
 * AND/OR/NOT, find phrases using stored positions, and rank by term frequency.
 *
 * Compile and run:
 *   javac InvertedIndex.java
 *   java InvertedIndex
 */
public class InvertedIndex {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Document(String id, String title, String body) {
    }

    /** Lowercase words and numbers; everything else is a separator. */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String piece : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!piece.isEmpty()) {
                tokens.add(piece);
            }
        }
        return tokens;
    }

    static final class Index {
        private final Map<String, Set<String>> postings = new TreeMap<>();
        private final Map<String, Map<String, List<Integer>>> positions = new TreeMap<>();
        private final Map<String, Document> documents = new LinkedHashMap<>();

        void add(Document document) {
            documents.put(document.id(), document);
            List<String> tokens = tokenize(document.title() + " " + document.body());
            for (int position = 0; position < tokens.size(); position++) {
                String term = tokens.get(position);
                postings.computeIfAbsent(term, key -> new TreeSet<>()).add(document.id());
                positions.computeIfAbsent(term, key -> new TreeMap<>())
                        .computeIfAbsent(document.id(), key -> new ArrayList<>())
                        .add(position);
            }
        }

        Set<String> search(String term) {
            return postings.getOrDefault(term.toLowerCase(), Set.of());
        }

        Set<String> and(String left, String right) {
            Set<String> result = new TreeSet<>(search(left));
            result.retainAll(search(right));
            return result;
        }

        Set<String> or(String left, String right) {
            Set<String> result = new TreeSet<>(search(left));
            result.addAll(search(right));
            return result;
        }

        Set<String> not(String term) {
            Set<String> result = new TreeSet<>(documents.keySet());
            result.removeAll(search(term));
            return result;
        }

        /** A phrase matches only when its terms appear consecutively, in order. */
        Set<String> phrase(String text) {
            List<String> terms = tokenize(text);
            if (terms.isEmpty()) {
                return Set.of();
            }
            Set<String> candidates = new TreeSet<>(search(terms.get(0)));
            for (String term : terms) {
                candidates.retainAll(search(term));
            }
            Set<String> found = new TreeSet<>();
            for (String id : candidates) {
                for (int start : positions.get(terms.get(0)).get(id)) {
                    boolean matched = true;
                    for (int offset = 1; offset < terms.size() && matched; offset++) {
                        List<Integer> where = positions.get(terms.get(offset)).get(id);
                        matched = where != null && where.contains(start + offset);
                    }
                    if (matched) {
                        found.add(id);
                        break;
                    }
                }
            }
            return found;
        }

        /** Term frequency for one term, per document. */
        Map<String, Integer> scores(String term) {
            Map<String, Integer> result = new TreeMap<>();
            positions.getOrDefault(term.toLowerCase(), Map.of())
                    .forEach((id, where) -> result.put(id, where.size()));
            return result;
        }

        /** Term frequencies summed across a query, sorted best first. */
        List<String> top(List<String> terms, int count) {
            Map<String, Integer> totals = new TreeMap<>();
            for (String term : terms) {
                scores(term).forEach((id, occurrences) -> totals.merge(id, occurrences, Integer::sum));
            }
            return totals.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(count)
                    .map(Map.Entry::getKey)
                    .toList();
        }

        Set<String> vocabulary() {
            return postings.keySet();
        }

        int size() {
            return documents.size();
        }

        int terms() {
            return postings.size();
        }
    }

    public static void main(String[] args) {
        Index index = new Index();
        index.add(new Document("d1", "Login tests",
                "the quick brown fox logs in with a valid password"));
        index.add(new Document("d2", "Checkout tests",
                "checkout accepts a valid card and shows the total"));
        index.add(new Document("d3", "Login failures",
                "an invalid password shows an error on the login page"));
        index.add(new Document("d4", "Search tests",
                "the quick search finds a product by name"));

        check(index.size() == 4, "four documents");
        check(index.vocabulary().contains("login") && index.vocabulary().contains("quick"),
                "the vocabulary holds the lowercased terms");
        System.out.println("terms       : " + index.terms() + " distinct");

        // ---- single term ------------------------------------------------------
        check(index.search("login").equals(Set.of("d1", "d3")), "login appears in d1 and d3");
        check(index.search("LOGIN").equals(Set.of("d1", "d3")), "lookups are case insensitive");
        check(index.search("missing").isEmpty(), "an unknown term finds nothing");
        check(index.search("valid").equals(Set.of("d1", "d2")), "valid appears in d1 and d2");
        System.out.println("search login: " + index.search("login"));

        // ---- boolean combination ---------------------------------------------
        check(index.and("login", "password").equals(Set.of("d1", "d3")), "login AND password");
        check(index.and("login", "card").isEmpty(), "login AND card finds nothing");
        check(index.or("card", "search").equals(Set.of("d2", "d4")), "card OR search");
        check(index.not("login").equals(Set.of("d2", "d4")), "NOT login");
        check(index.not("quick").equals(Set.of("d2", "d3")), "NOT quick");
        System.out.println("login AND password: " + index.and("login", "password"));

        // ---- phrases ----------------------------------------------------------
        check(index.phrase("quick brown").equals(Set.of("d1")),
                "the phrase quick brown only occurs in d1");
        check(index.phrase("quick search").equals(Set.of("d4")), "quick search only in d4");
        check(index.phrase("brown quick").isEmpty(), "reversing the phrase finds nothing");
        check(index.phrase("valid password").equals(Set.of("d1")),
                "valid password is in d1 only, because d3 has invalid");
        check(index.phrase("login").equals(Set.of("d1", "d3")), "a one-word phrase is just a term");
        check(index.phrase("").isEmpty(), "an empty phrase matches nothing");
        System.out.println("phrase quick brown: " + index.phrase("quick brown"));

        // ---- ranking ----------------------------------------------------------
        check(index.scores("quick").equals(Map.of("d1", 1, "d4", 1)), "term frequency per document");
        check(index.top(List.of("quick", "login"), 2).equals(List.of("d1", "d3")),
                "a tie on total frequency breaks alphabetically");
        check(index.scores("login").equals(Map.of("d1", 1, "d3", 2)),
                "d3 mentions login twice: once in its title, once in its body");
        check(index.top(List.of("login"), 1).equals(List.of("d3")),
                "so term frequency ranks d3 first");
        check(index.top(List.of("nothing"), 3).isEmpty(), "no hits, no ranking");
        System.out.println("top quick+login: " + index.top(List.of("quick", "login"), 4));

        // ---- tokenising -------------------------------------------------------
        check(tokenize("Hello, World! 42").equals(List.of("hello", "world", "42")),
                "punctuation is dropped and digits are kept");
        check(tokenize("").isEmpty(), "an empty string has no tokens");
        check(tokenize("...").isEmpty(), "punctuation only produces nothing");

        // ---- a document added later is searchable straight away ---------------
        index.add(new Document("d5", "Password reset", "a user can reset a forgotten password"));
        check(index.size() == 5, "the fifth document was added");
        check(index.search("password").equals(Set.of("d1", "d3", "d5")), "and is picked up by the index");
        check(index.phrase("forgotten password").equals(Set.of("d5")), "its phrase is indexed too");
        System.out.println("password in : " + index.search("password"));
        System.out.println("All checks passed.");
    }
}
