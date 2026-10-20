package com.campuslostfound.model;

import java.time.LocalDate;

/**
 * Represents a report of an item found on campus (FOUND_ITEMS table).
 */
public class FoundItem {

    private int foundId;
    private String itemName;
    private String category;
    private String description;
    private String location;
    private LocalDate dateFound;
    private String contactName;
    private String contactInfo;
    private ItemStatus status;

    public FoundItem() {
        this.status = ItemStatus.OPEN;
    }

    public FoundItem(int foundId, String itemName, String category, String description,
                     String location, LocalDate dateFound, String contactName,
                     String contactInfo, ItemStatus status) {
        this.foundId = foundId;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.location = location;
        this.dateFound = dateFound;
        this.contactName = contactName;
        this.contactInfo = contactInfo;
        this.status = status;
    }

    public int getFoundId() {
        return foundId;
    }

    public void setFoundId(int foundId) {
        this.foundId = foundId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDateFound() {
        return dateFound;
    }

    public void setDateFound(LocalDate dateFound) {
        this.dateFound = dateFound;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
                "| %-4d | %-20s | %-14s | %-20s | %-12s | %-10s | %-14s | %-8s |",
                foundId, itemName, category, location, String.valueOf(dateFound),
                contactName, contactInfo, status);
    }

    public static String header() {
        return String.format(
                "| %-4s | %-20s | %-14s | %-20s | %-12s | %-10s | %-14s | %-8s |",
                "ID", "Item Name", "Category", "Location", "Date", "Contact",
                "Contact Info", "Status");
    }

    public String toDetailString() {
        return String.format(
                "FOUND #%d | %s | %s%n  Description : %s%n  Location    : %s%n  Date found  : %s%n  Contact     : %s (%s)%n  Status      : %s",
                foundId, itemName, category,
                description == null || description.isBlank() ? "(none)" : description,
                location == null || location.isBlank() ? "(none)" : location,
                dateFound, contactName, contactInfo, status);
    }
}
