package com.campuslostfound.service;

import com.campuslostfound.dao.FoundItemDAO;
import com.campuslostfound.dao.LostItemDAO;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.LostItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Smart matching engine.
 *
 * Compares lost-item reports against found-item reports and computes a
 * similarity score (0-100) from four signals:
 *   - Category match            (25 points)
 *   - Location similarity       (20 points)
 *   - Date proximity            (20 points)
 *   - Description/name keywords (35 points, Jaccard overlap)
 *
 * Pairs scoring at or above the threshold are suggested as possible matches.
 */
@Service
public class MatchingService {

    /** Minimum score for a pair to be suggested. */
    public static final int MATCH_THRESHOLD = 40;

    private static final int CATEGORY_WEIGHT = 25;
    private static final int LOCATION_WEIGHT = 20;
    private static final int DATE_WEIGHT = 20;
    private static final int KEYWORD_WEIGHT = 35;

    private final LostItemDAO lostItemDao;
    private final FoundItemDAO foundItemDao;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "of", "and", "or", "on", "in", "at", "to", "is",
            "it", "for", "with", "my", "me", "i", "this", "that", "was", "from",
            "near", "has", "have", "had", "be", "by", "as", "are", "its"));

    public MatchingService(LostItemDAO lostItemDao, FoundItemDAO foundItemDao) {
        this.lostItemDao = lostItemDao;
        this.foundItemDao = foundItemDao;
    }

    /**
     * Compares a specific lost item against all OPEN found items.
     */
    public List<MatchResult> matchLostItem(int lostId) throws SQLException {
        LostItem lost = lostItemDao.findById(lostId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lost item #" + lostId + " does not exist."));
        List<MatchResult> results = new ArrayList<>();
        for (FoundItem found : foundItemDao.findOpen()) {
            int score = similarity(lost, found);
            if (score >= MATCH_THRESHOLD) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    /**
     * Compares a specific found item against all OPEN lost items.
     */
    public List<MatchResult> matchFoundItem(int foundId) throws SQLException {
        FoundItem found = foundItemDao.findById(foundId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Found item #" + foundId + " does not exist."));
        List<MatchResult> results = new ArrayList<>();
        for (LostItem lost : lostItemDao.findOpen()) {
            int score = similarity(lost, found);
            if (score >= MATCH_THRESHOLD) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    /**
     * Runs smart matching across every OPEN lost and OPEN found report.
     */
    public List<MatchResult> matchAllOpenReports() throws SQLException {
        List<MatchResult> results = new ArrayList<>();
        for (LostItem lost : lostItemDao.findOpen()) {
            for (FoundItem found : foundItemDao.findOpen()) {
                int score = similarity(lost, found);
                if (score >= MATCH_THRESHOLD) {
                    results.add(new MatchResult(lost, found, score));
                }
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    /**
     * Scores two in-memory items that are not yet persisted (used right after
     * a report is created, before the user leaves the screen).
     */
    public List<MatchResult> matchUnsavedLostAgainstOpenFound(LostItem lost) throws SQLException {
        List<MatchResult> results = new ArrayList<>();
        for (FoundItem found : foundItemDao.findOpen()) {
            int score = similarity(lost, found);
            if (score >= MATCH_THRESHOLD) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    public List<MatchResult> matchUnsavedFoundAgainstOpenLost(FoundItem found) throws SQLException {
        List<MatchResult> results = new ArrayList<>();
        for (LostItem lost : lostItemDao.findOpen()) {
            int score = similarity(lost, found);
            if (score >= MATCH_THRESHOLD) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    /**
     * Computes the overall similarity score (0-100) between a lost and a
     * found report.
     */
    public int similarity(LostItem lost, FoundItem found) {
        int score = 0;
        score += categoryScore(lost.getCategory(), found.getCategory());
        score += locationScore(lost.getLocation(), found.getLocation());
        score += dateScore(lost.getDateLost() == null ? null : lost.getDateLost().toEpochDay(),
                found.getDateFound() == null ? null : found.getDateFound().toEpochDay());
        score += keywordScore(lost.getItemName(), lost.getDescription(),
                found.getItemName(), found.getDescription());
        return Math.min(100, score);
    }

    /**
     * Exact category match (case-insensitive) earns full points.
     * Sub-string containment earns partial credit.
     */
    private int categoryScore(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0;
        }
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb)) {
            return CATEGORY_WEIGHT;
        }
        if (na.contains(nb) || nb.contains(na)) {
            return CATEGORY_WEIGHT / 2;
        }
        return 0;
    }

    /**
     * Location earns full points for an exact match, partial for shared
     * tokens or containment.
     */
    private int locationScore(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0;
        }
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb)) {
            return LOCATION_WEIGHT;
        }
        if (na.contains(nb) || nb.contains(na)) {
            return (int) Math.round(LOCATION_WEIGHT * 0.75);
        }
        double jaccard = jaccard(tokenize(a), tokenize(b));
        return (int) Math.round(LOCATION_WEIGHT * jaccard);
    }

    /**
     * Date proximity: same day earns full points; the score decays over
     * 3, 7 and 14 days.
     */
    private int dateScore(Long daysA, Long daysB) {
        if (daysA == null || daysB == null) {
            return 0;
        }
        long diff = Math.abs(daysA - daysB);
        if (diff == 0) {
            return DATE_WEIGHT;
        }
        if (diff <= 3) {
            return (int) Math.round(DATE_WEIGHT * 0.75);
        }
        if (diff <= 7) {
            return DATE_WEIGHT / 2;
        }
        if (diff <= 14) {
            return DATE_WEIGHT / 4;
        }
        return 0;
    }

    /**
     * Keyword overlap between item name + description, using Jaccard
     * similarity over tokenized text.
     */
    private int keywordScore(String nameA, String descA, String nameB, String descB) {
        Set<String> tokensA = tokenize(nameA, descA);
        Set<String> tokensB = tokenize(nameB, descB);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0;
        }
        return (int) Math.round(KEYWORD_WEIGHT * jaccard(tokensA, tokensB));
    }

    /**
     * Jaccard index: |A intersect B| / |A union B|.
     */
    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Lowercases, strips punctuation and removes stop words.
     */
    private Set<String> tokenize(String... parts) {
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String[] words = part.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
            for (String word : words) {
                if (word.length() > 1 && !STOP_WORDS.contains(word)) {
                    tokens.add(word);
                }
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /**
     * Human-readable breakdown of how the score was computed. Useful for
    / debugging and for showing users why two reports match.
     */
    public String explain(LostItem lost, FoundItem found) {
        int cat = categoryScore(lost.getCategory(), found.getCategory());
        int loc = locationScore(lost.getLocation(), found.getLocation());
        int dat = dateScore(lost.getDateLost() == null ? null : lost.getDateLost().toEpochDay(),
                found.getDateFound() == null ? null : found.getDateFound().toEpochDay());
        int key = keywordScore(lost.getItemName(), lost.getDescription(),
                found.getItemName(), found.getDescription());
        return String.format(
                "    breakdown: category %d/%d | location %d/%d | date %d/%d | keywords %d/%d",
                cat, CATEGORY_WEIGHT, loc, LOCATION_WEIGHT,
                dat, DATE_WEIGHT, key, KEYWORD_WEIGHT);
    }
}
