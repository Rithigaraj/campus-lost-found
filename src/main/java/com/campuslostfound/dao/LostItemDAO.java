package com.campuslostfound.dao;

import com.campuslostfound.db.DatabaseManager;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;

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
 * Data access object for the LOST_ITEMS table.
 * All database operations for lost-item reports go through this class.
 */
@Component
public class LostItemDAO {

    private static final String COLS =
            "lost_id, item_name, category, description, location, "
            + "date_lost, contact_name, contact_info, status";

    /**
     * Inserts a new lost-item report and returns the generated lost_id.
     */
    public int insert(LostItem item) throws SQLException {
        String sql = "INSERT INTO LOST_ITEMS "
                + "(item_name, category, description, location, date_lost, contact_name, contact_info, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getLocation());
            ps.setDate(5, Date.valueOf(item.getDateLost()));
            ps.setString(6, item.getContactName());
            ps.setString(7, item.getContactInfo());
            ps.setString(8, item.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    item.setLostId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed: no generated key returned.");
    }

    /**
     * Returns every lost-item report, ordered by lost_id.
     */
    public List<LostItem> findAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM LOST_ITEMS ORDER BY lost_id";
        List<LostItem> items = new ArrayList<>();
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
     * Returns every OPEN lost-item report (used by smart matching).
     */
    public List<LostItem> findOpen() throws SQLException {
        String sql = "SELECT " + COLS + " FROM LOST_ITEMS WHERE status = 'OPEN' ORDER BY lost_id";
        List<LostItem> items = new ArrayList<>();
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
     * Finds a lost item by its primary key. Empty when not found.
     */
    public Optional<LostItem> findById(int lostId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM LOST_ITEMS WHERE lost_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lostId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates all editable fields of an existing lost-item report.
     */
    public boolean update(LostItem item) throws SQLException {
        String sql = "UPDATE LOST_ITEMS SET item_name = ?, category = ?, description = ?, "
                + "location = ?, date_lost = ?, contact_name = ?, contact_info = ?, status = ? "
                + "WHERE lost_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getLocation());
            ps.setDate(5, Date.valueOf(item.getDateLost()));
            ps.setString(6, item.getContactName());
            ps.setString(7, item.getContactInfo());
            ps.setString(8, item.getStatus().name());
            ps.setInt(9, item.getLostId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates only the status of a lost-item report.
     */
    public boolean updateStatus(int lostId, ItemStatus status) throws SQLException {
        String sql = "UPDATE LOST_ITEMS SET status = ? WHERE lost_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, lostId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a lost-item report by id.
     */
    public boolean delete(int lostId) throws SQLException {
        String sql = "DELETE FROM LOST_ITEMS WHERE lost_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lostId);
            return ps.executeUpdate() > 0;
        }
    }

    private LostItem mapRow(ResultSet rs) throws SQLException {
        LocalDate dateLost = rs.getDate("date_lost") != null
                ? rs.getDate("date_lost").toLocalDate() : null;
        return new LostItem(
                rs.getInt("lost_id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("location"),
                dateLost,
                rs.getString("contact_name"),
                rs.getString("contact_info"),
                ItemStatus.fromString(rs.getString("status")));
    }
}
