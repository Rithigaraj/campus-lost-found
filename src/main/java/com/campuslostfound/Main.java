package com.campuslostfound;

import com.campuslostfound.dao.FoundItemDAO;
import com.campuslostfound.dao.LostItemDAO;
import com.campuslostfound.db.DatabaseManager;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.service.MatchResult;
import com.campuslostfound.service.MatchingService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu-driven console front-end for the Campus Lost &amp; Found Network.
 *
 * Functionalities:
 *   1. Report Lost Item      5. Update Reports
 *   2. Report Found Item     6. Delete Reports
 *   3. View Reports          7. Mark as Returned
 *   4. Smart Matching        0. Exit
 */
public class Main {

    private final Scanner scanner = new Scanner(System.in);
    private final LostItemDAO lostDao = new LostItemDAO();
    private final FoundItemDAO foundDao = new FoundItemDAO();
    private final MatchingService matching = new MatchingService(lostDao, foundDao);

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        println("");
        println("======================================================");
        println("       CAMPUS LOST & FOUND NETWORK");
        println("======================================================");

        if (!DatabaseManager.testConnection()) {
            println("ERROR: Cannot connect to the MySQL database.");
            println("Please make sure MySQL is running and src/main/resources");
            println("db.properties contains the correct url / user / password,");
            println("then run schema.sql to create the campus_lost_found database.");
            return;
        }
        println("Database connection established.\n");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Choose an option: ").trim();
            println("");
            try {
                switch (choice) {
                    case "1" -> reportLostItem();
                    case "2" -> reportFoundItem();
                    case "3" -> viewReports();
                    case "4" -> smartMatchingMenu();
                    case "5" -> updateReports();
                    case "6" -> deleteReports();
                    case "7" -> markAsReturned();
                    case "0" -> {
                        running = false;
                        println("Goodbye!");
                    }
                    default -> println("Invalid option. Please choose 0-7.");
                }
            } catch (EndOfInputException e) {
                println("\nInput closed. Goodbye!");
                break;
            } catch (SQLException e) {
                println("Database error: " + e.getMessage());
            } catch (Exception e) {
                println("Error: " + e.getMessage());
            }
            println("");
        }
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    private void printMenu() {
        println("------------------------------------------------------");
        println(" 1. Report Lost Item");
        println(" 2. Report Found Item");
        println(" 3. View Reports");
        println(" 4. Smart Matching");
        println(" 5. Update Reports");
        println(" 6. Delete Reports");
        println(" 7. Mark as Returned");
        println(" 0. Exit");
        println("------------------------------------------------------");
    }

    // ------------------------------------------------------------------
    // 1. Report Lost Item
    // ------------------------------------------------------------------

    private void reportLostItem() throws SQLException {
        println("--- Report a Lost Item ---");
        LostItem item = new LostItem();
        item.setItemName(required("Item name: "));
        item.setCategory(required("Category (e.g. Electronics, Books, Keys, ID Card): "));
        item.setDescription(readLine("Description: "));
        item.setLocation(readLine("Last seen location: "));
        item.setDateLost(readDate("Date lost (YYYY-MM-DD): "));
        item.setContactName(required("Contact name: "));
        item.setContactInfo(required("Contact info (phone/email): "));
        item.setStatus(ItemStatus.OPEN);

        int id = lostDao.insert(item);
        println("\nLost item reported successfully. Lost ID = " + id);

        suggestMatchesForNewLost(item);
    }

    // ------------------------------------------------------------------
    // 2. Report Found Item
    // ------------------------------------------------------------------

    private void reportFoundItem() throws SQLException {
        println("--- Report a Found Item ---");
        FoundItem item = new FoundItem();
        item.setItemName(required("Item name: "));
        item.setCategory(required("Category (e.g. Electronics, Books, Keys, ID Card): "));
        item.setDescription(readLine("Description: "));
        item.setLocation(readLine("Found location: "));
        item.setDateFound(readDate("Date found (YYYY-MM-DD): "));
        item.setContactName(required("Contact name: "));
        item.setContactInfo(required("Contact info (phone/email): "));
        item.setStatus(ItemStatus.OPEN);

        int id = foundDao.insert(item);
        println("\nFound item reported successfully. Found ID = " + id);

        suggestMatchesForNewFound(item);
    }

    private void suggestMatchesForNewLost(LostItem lost) throws SQLException {
        List<MatchResult> matches = matching.matchUnsavedLostAgainstOpenFound(lost);
        if (matches.isEmpty()) {
            println("No possible matches found among open found-items right now.");
            return;
        }
        println("\nPossible matches with OPEN found-items:");
        printMatches(matches);
        offerToConfirmMatch(matches);
    }

    private void suggestMatchesForNewFound(FoundItem found) throws SQLException {
        List<MatchResult> matches = matching.matchUnsavedFoundAgainstOpenLost(found);
        if (matches.isEmpty()) {
            println("No possible matches found among open lost-items right now.");
            return;
        }
        println("\nPossible matches with OPEN lost-items:");
        printMatches(matches);
        offerToConfirmMatch(matches);
    }

    // ------------------------------------------------------------------
    // 3. View Reports
    // ------------------------------------------------------------------

    private void viewReports() throws SQLException {
        println("--- View Reports ---");
        println("1. Lost items only");
        println("2. Found items only");
        println("3. Both");
        String choice = readLine("Choose: ").trim();

        switch (choice) {
            case "1" -> viewLostItems();
            case "2" -> viewFoundItems();
            case "3" -> {
                viewLostItems();
                println("");
                viewFoundItems();
            }
            default -> println("Invalid option.");
        }
    }

    private void viewLostItems() throws SQLException {
        List<LostItem> items = lostDao.findAll();
        println("LOST ITEMS (" + items.size() + " report(s))");
        println("-".repeat(120));
        println(LostItem.header());
        println("-".repeat(120));
        for (LostItem item : items) {
            println(item.toString());
        }
        if (items.isEmpty()) {
            println("(no lost items recorded)");
        }
        println("-".repeat(120));
    }

    private void viewFoundItems() throws SQLException {
        List<FoundItem> items = foundDao.findAll();
        println("FOUND ITEMS (" + items.size() + " report(s))");
        println("-".repeat(120));
        println(FoundItem.header());
        println("-".repeat(120));
        for (FoundItem item : items) {
            println(item.toString());
        }
        if (items.isEmpty()) {
            println("(no found items recorded)");
        }
        println("-".repeat(120));
    }

    // ------------------------------------------------------------------
    // 4. Smart Matching
    // ------------------------------------------------------------------

    private void smartMatchingMenu() throws SQLException {
        println("--- Smart Matching ---");
        println("1. Match a lost item against open found items");
        println("2. Match a found item against open lost items");
        println("3. Match all open lost and found reports");
        String choice = readLine("Choose: ").trim();

        switch (choice) {
            case "1" -> {
                int id = readInt("Enter Lost ID: ");
                List<MatchResult> matches = matching.matchLostItem(id);
                if (matches.isEmpty()) {
                    println("No matches at or above the similarity threshold ("
                            + MatchingService.MATCH_THRESHOLD + "/100).");
                } else {
                    println("Possible matches for lost item #" + id + ":");
                    printMatches(matches);
                    offerToConfirmMatch(matches);
                }
            }
            case "2" -> {
                int id = readInt("Enter Found ID: ");
                List<MatchResult> matches = matching.matchFoundItem(id);
                if (matches.isEmpty()) {
                    println("No matches at or above the similarity threshold ("
                            + MatchingService.MATCH_THRESHOLD + "/100).");
                } else {
                    println("Possible matches for found item #" + id + ":");
                    printMatches(matches);
                    offerToConfirmMatch(matches);
                }
            }
            case "3" -> {
                List<MatchResult> matches = matching.matchAllOpenReports();
                if (matches.isEmpty()) {
                    println("No matches at or above the similarity threshold ("
                            + MatchingService.MATCH_THRESHOLD + "/100).");
                } else {
                    println("All possible matches between open reports ("
                            + matches.size() + " pair(s)):");
                    printMatches(matches);
                    offerToConfirmMatch(matches);
                }
            }
            default -> println("Invalid option.");
        }
    }

    private void printMatches(List<MatchResult> matches) {
        println("-".repeat(100));
        for (int i = 0; i < matches.size(); i++) {
            MatchResult m = matches.get(i);
            println(String.format("[%d] %s", i + 1, m));
            println(matching.explain(m.getLostItem(), m.getFoundItem()));
        }
        println("-".repeat(100));
    }

    /**
     * After showing suggestions, lets the user confirm a pair. On
     * confirmation both reports are set to MATCHED.
     */
    private void offerToConfirmMatch(List<MatchResult> matches) throws SQLException {
        String answer = readLine("Confirm a match? (y/n): ").trim();
        if (!"y".equalsIgnoreCase(answer)) {
            return;
        }
        int index = readInt("Enter the match number to confirm: ");
        if (index < 1 || index > matches.size()) {
            println("Invalid match number.");
            return;
        }
        MatchResult chosen = matches.get(index - 1);
        lostDao.updateStatus(chosen.getLostItem().getLostId(), ItemStatus.MATCHED);
        foundDao.updateStatus(chosen.getFoundItem().getFoundId(), ItemStatus.MATCHED);
        println("Match confirmed. Lost #" + chosen.getLostItem().getLostId()
                + " and Found #" + chosen.getFoundItem().getFoundId()
                + " are now marked MATCHED.");
        println("Contact " + chosen.getLostItem().getContactName() + " ("
                + chosen.getLostItem().getContactInfo() + ") and "
                + chosen.getFoundItem().getContactName() + " ("
                + chosen.getFoundItem().getContactInfo()
                + ") to arrange the return, then use option 7 to mark as RETURNED.");
    }

    // ------------------------------------------------------------------
    // 5. Update Reports
    // ------------------------------------------------------------------

    private void updateReports() throws SQLException {
        println("--- Update Reports ---");
        String type = readLine("Update (L)ost or (F)ound item? ").trim();
        if (type.isEmpty()) {
            println("No choice made.");
            return;
        }

        if (type.equalsIgnoreCase("L")) {
            int id = readInt("Enter Lost ID to update: ");
            Optional<LostItem> found = lostDao.findById(id);
            if (found.isEmpty()) {
                println("Lost item #" + id + " not found.");
                return;
            }
            LostItem item = found.get();
            println("Current record:");
            println(item.toDetailString());
            editLostItem(item);
            if (lostDao.update(item)) {
                println("Lost item #" + id + " updated.");
            } else {
                println("Update failed.");
            }
        } else if (type.equalsIgnoreCase("F")) {
            int id = readInt("Enter Found ID to update: ");
            Optional<FoundItem> found = foundDao.findById(id);
            if (found.isEmpty()) {
                println("Found item #" + id + " not found.");
                return;
            }
            FoundItem item = found.get();
            println("Current record:");
            println(item.toDetailString());
            editFoundItem(item);
            if (foundDao.update(item)) {
                println("Found item #" + id + " updated.");
            } else {
                println("Update failed.");
            }
        } else {
            println("Enter L or F only.");
        }
    }

    private void editLostItem(LostItem item) {
        println("Press ENTER to keep the current value shown in [brackets].");
        String value;
        value = readLine("  Item name [" + item.getItemName() + "]: ");
        if (!value.isBlank()) {
            item.setItemName(value.trim());
        }
        value = readLine("  Category [" + item.getCategory() + "]: ");
        if (!value.isBlank()) {
            item.setCategory(value.trim());
        }
        value = readLine("  Description [" + nullSafe(item.getDescription()) + "]: ");
        if (!value.isBlank()) {
            item.setDescription(value.trim());
        }
        value = readLine("  Location [" + nullSafe(item.getLocation()) + "]: ");
        if (!value.isBlank()) {
            item.setLocation(value.trim());
        }
        value = readLine("  Date lost [" + item.getDateLost() + "] (YYYY-MM-DD): ");
        if (!value.isBlank()) {
            item.setDateLost(parseDate(value));
        }
        value = readLine("  Contact name [" + item.getContactName() + "]: ");
        if (!value.isBlank()) {
            item.setContactName(value.trim());
        }
        value = readLine("  Contact info [" + item.getContactInfo() + "]: ");
        if (!value.isBlank()) {
            item.setContactInfo(value.trim());
        }
        value = readLine("  Status [" + item.getStatus() + "] (OPEN/MATCHED/RETURNED/CLOSED): ");
        if (!value.isBlank()) {
            ItemStatus status = ItemStatus.fromString(value);
            if (status == null) {
                println("  Invalid status, keeping " + item.getStatus() + ".");
            } else {
                item.setStatus(status);
            }
        }
    }

    private void editFoundItem(FoundItem item) {
        println("Press ENTER to keep the current value shown in [brackets].");
        String value;
        value = readLine("  Item name [" + item.getItemName() + "]: ");
        if (!value.isBlank()) {
            item.setItemName(value.trim());
        }
        value = readLine("  Category [" + item.getCategory() + "]: ");
        if (!value.isBlank()) {
            item.setCategory(value.trim());
        }
        value = readLine("  Description [" + nullSafe(item.getDescription()) + "]: ");
        if (!value.isBlank()) {
            item.setDescription(value.trim());
        }
        value = readLine("  Location [" + nullSafe(item.getLocation()) + "]: ");
        if (!value.isBlank()) {
            item.setLocation(value.trim());
        }
        value = readLine("  Date found [" + item.getDateFound() + "] (YYYY-MM-DD): ");
        if (!value.isBlank()) {
            item.setDateFound(parseDate(value));
        }
        value = readLine("  Contact name [" + item.getContactName() + "]: ");
        if (!value.isBlank()) {
            item.setContactName(value.trim());
        }
        value = readLine("  Contact info [" + item.getContactInfo() + "]: ");
        if (!value.isBlank()) {
            item.setContactInfo(value.trim());
        }
        value = readLine("  Status [" + item.getStatus() + "] (OPEN/MATCHED/RETURNED/CLOSED): ");
        if (!value.isBlank()) {
            ItemStatus status = ItemStatus.fromString(value);
            if (status == null) {
                println("  Invalid status, keeping " + item.getStatus() + ".");
            } else {
                item.setStatus(status);
            }
        }
    }

    // ------------------------------------------------------------------
    // 6. Delete Reports
    // ------------------------------------------------------------------

    private void deleteReports() throws SQLException {
        println("--- Delete Reports ---");
        String type = readLine("Delete a (L)ost or (F)ound report? ").trim();
        if (type.equalsIgnoreCase("L")) {
            int id = readInt("Enter Lost ID to delete: ");
            Optional<LostItem> item = lostDao.findById(id);
            if (item.isEmpty()) {
                println("Lost item #" + id + " not found.");
                return;
            }
            println(item.get().toDetailString());
            String confirm = readLine("Really delete this report? (y/n): ").trim();
            if ("y".equalsIgnoreCase(confirm)) {
                lostDao.delete(id);
                println("Lost item #" + id + " deleted.");
            } else {
                println("Delete cancelled.");
            }
        } else if (type.equalsIgnoreCase("F")) {
            int id = readInt("Enter Found ID to delete: ");
            Optional<FoundItem> item = foundDao.findById(id);
            if (item.isEmpty()) {
                println("Found item #" + id + " not found.");
                return;
            }
            println(item.get().toDetailString());
            String confirm = readLine("Really delete this report? (y/n): ").trim();
            if ("y".equalsIgnoreCase(confirm)) {
                foundDao.delete(id);
                println("Found item #" + id + " deleted.");
            } else {
                println("Delete cancelled.");
            }
        } else {
            println("Enter L or F only.");
        }
    }

    // ------------------------------------------------------------------
    // 7. Mark as Returned
    // ------------------------------------------------------------------

    private void markAsReturned() throws SQLException {
        println("--- Mark as Returned ---");
        String type = readLine("Mark a (L)ost or (F)ound report as returned? ").trim();
        if (type.equalsIgnoreCase("L")) {
            int id = readInt("Enter Lost ID: ");
            Optional<LostItem> item = lostDao.findById(id);
            if (item.isEmpty()) {
                println("Lost item #" + id + " not found.");
                return;
            }
            println(item.get().toDetailString());
            String confirm = readLine("Mark as RETURNED? (y/n): ").trim();
            if ("y".equalsIgnoreCase(confirm)) {
                lostDao.updateStatus(id, ItemStatus.RETURNED);
                println("Lost item #" + id + " marked as RETURNED.");
            } else {
                println("Cancelled.");
            }
        } else if (type.equalsIgnoreCase("F")) {
            int id = readInt("Enter Found ID: ");
            Optional<FoundItem> item = foundDao.findById(id);
            if (item.isEmpty()) {
                println("Found item #" + id + " not found.");
                return;
            }
            println(item.get().toDetailString());
            String confirm = readLine("Mark as RETURNED? (y/n): ").trim();
            if ("y".equalsIgnoreCase(confirm)) {
                foundDao.updateStatus(id, ItemStatus.RETURNED);
                println("Found item #" + id + " marked as RETURNED.");
            } else {
                println("Cancelled.");
            }
        } else {
            println("Enter L or F only.");
        }
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    /** Thrown when stdin is closed (EOF) so the app can exit cleanly. */
    private static final class EndOfInputException extends RuntimeException {
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();
        }
        return scanner.nextLine();
    }

    private String required(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            println("  This field is required.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            String raw = readLine(prompt).trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                println("  Please enter a whole number.");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String raw = readLine(prompt).trim();
            try {
                return parseDate(raw);
            } catch (DateTimeParseException e) {
                println("  Invalid date. Use YYYY-MM-DD, e.g. 2026-10-12.");
            }
        }
    }

    private LocalDate parseDate(String raw) {
        return LocalDate.parse(raw.trim());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void println(String message) {
        System.out.println(message);
    }
}
