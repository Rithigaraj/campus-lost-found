package com.campuslostfound.service;

import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.LostItem;

/**
 * Holds a possible match between a lost-item report and a found-item report,
 * together with the similarity score (0-100) computed by MatchingService.
 */
public class MatchResult {

    private final LostItem lostItem;
    private final FoundItem foundItem;
    private final int score;

    public MatchResult(LostItem lostItem, FoundItem foundItem, int score) {
        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.score = score;
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public FoundItem getFoundItem() {
        return foundItem;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format(
                "Score %3d/100  ->  LOST #%d \"%s\" (%s, %s)  <-->  FOUND #%d \"%s\" (%s, %s)",
                score,
                lostItem.getLostId(), lostItem.getItemName(),
                lostItem.getCategory(), lostItem.getLocation(),
                foundItem.getFoundId(), foundItem.getItemName(),
                foundItem.getCategory(), foundItem.getLocation());
    }
}
