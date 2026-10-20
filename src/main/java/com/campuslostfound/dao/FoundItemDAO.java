package com.campuslostfound.dao;

import com.campuslostfound.db.DatabaseManager;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.ItemStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Data access object for the FOUND_ITEMS table.
 * All database operations for found-item reports go through this class.
 */
@Component
public class FoundItemDAO {

    private static final String COLS =
            "found_id, item_name, category, description, location, "
            + "date_found, contact_name, contact_info, status";

    /**
     * Inserts a new found-item report and returns the generated found_id.
     */
    public int insert(FoundItem item) throws SQLException {
        String sql = "INSERT INTO FOUND_ITEMS "
                + "(item_name, category, description, location, date_found, contact_name, contact_info, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getLocation());
            ps.setDate(5, Date.valueOf(item.getDateFound()));
            ps.setString(6, item.getContactName());
            ps.setString(7, item.getContactInfo());
            ps.setString(8, item.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    item.setFoundId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed: no generated key returned.");
    }

    /**
     * Returns every found-item report, ordered by found_id.
     */
    public List<FoundItem> findAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM FOUND_ITEMS ORDER BY found_id";
        List<FoundItem> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    /**
     * Returns every OPEN found-item report (used by smart matching).
     */
    public List<FoundItem> findOpen() throws SQLException {
        String sql = "SELECT " + COLS + " FROM FOUND_ITEMS WHERE status = 'OPEN' ORDER BY found_id";
        List<FoundItem> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    /**
     * Finds a found item by its primary key. Empty when not found.
     */
    public Optional<FoundItem> findById(int foundId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM FOUND_ITEMS WHERE found_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates all editable fields of an existing found-item report.
     */
    public boolean update(FoundItem item) throws SQLException {
        String sql = "UPDATE FOUND_ITEMS SET item_name = ?, category = ?, description = ?, "
                + "location = ?, date_found = ?, contact_name = ?, contact_info = ?, status = ? "
                + "WHERE found_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getLocation());
            ps.setDate(5, Date.valueOf(item.getDateFound()));
            ps.setString(6, item.getContactName());
            ps.setString(7, item.getContactInfo());
            ps.setString(8, item.getStatus().name());
            ps.setInt(9, item.getFoundId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates only the status of a found-item report.
     */
    public boolean updateStatus(int foundId, ItemStatus status) throws SQLException {
        String sql = "UPDATE FOUND_ITEMS SET status = ? WHERE found_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, foundId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a found-item report by id.
     */
    public boolean delete(int foundId) throws SQLException {
        String sql = "DELETE FROM FOUND_ITEMS WHERE found_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foundId);
            return ps.executeUpdate() > 0;
        }
    }

    private FoundItem mapRow(ResultSet rs) throws SQLException {
        LocalDate dateFound = rs.getDate("date_found") != null
                ? rs.getDate("date_found").toLocalDate() : null;
        return new FoundItem(
                rs.getInt("found_id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("location"),
                dateFound,
                rs.getString("contact_name"),
                rs.getString("contact_info"),
                ItemStatus.fromString(rs.getString("status")));
    }
}
