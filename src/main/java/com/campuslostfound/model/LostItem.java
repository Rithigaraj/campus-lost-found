package com.campuslostfound.model;

import java.time.LocalDate;

/**
 * Represents a report of an item lost on campus (LOST_ITEMS table).
 */
public class LostItem {

    private int lostId;
    private String itemName;
    private String category;
    private String description;
    private String location;
    private LocalDate dateLost;
    private String contactName;
    private String contactInfo;
    private ItemStatus status;

    public LostItem() {
        this.status = ItemStatus.OPEN;
    }

    public LostItem(int lostId, String itemName, String category, String description,
                    String location, LocalDate dateLost, String contactName,
                    String contactInfo, ItemStatus status) {
        this.lostId = lostId;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.location = location;
        this.dateLost = dateLost;
        this.contactName = contactName;
        this.contactInfo = contactInfo;
        this.status = status;
    }

    public int getLostId() {
        return lostId;
    }

    public void setLostId(int lostId) {
        this.lostId = lostId;
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

    public LocalDate getDateLost() {
        return dateLost;
    }

    public void setDateLost(LocalDate dateLost) {
        this.dateLost = dateLost;
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
                "| %-4d | %-20s | %-14s | %-20s | %-12s | %-10s | %-14s | %-12s | %-8s |",
                lostId, itemName, category, location, String.valueOf(dateLost),
                contactName, contactInfo, "-", status);
    }

    public static String header() {
        return String.join(" ",
                String.format("| %-4s | %-20s | %-14s | %-20s | %-12s | %-10s | %-14s | %-12s | %-8s |",
                        "ID", "Item Name", "Category", "Location", "Date", "Contact",
                        "Contact Info", "Desc", "Status"));
    }

    public String toDetailString() {
        return String.format(
                "LOST #%d | %s | %s%n  Description : %s%n  Location    : %s%n  Date lost   : %s%n  Contact     : %s (%s)%n  Status      : %s",
                lostId, itemName, category,
                description == null || description.isBlank() ? "(none)" : description,
                location == null || location.isBlank() ? "(none)" : location,
                dateLost, contactName, contactInfo, status);
    }
}
