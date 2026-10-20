package com.campuslostfound.web;

import com.campuslostfound.dao.LostItemDAO;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

/**
 * REST API for lost-item reports (LOST_ITEMS table).
 *
 * GET    /api/lost             list all reports
 * GET    /api/lost/{id}        get one report
 * POST   /api/lost             create a report
 * PUT    /api/lost/{id}        update a report
 * PATCH  /api/lost/{id}/status update only the status
 * DELETE /api/lost/{id}        delete a report
 */
@RestController
@RequestMapping("/api/lost")
public class LostItemController {

    private final LostItemDAO lostDao;

    public LostItemController(LostItemDAO lostDao) {
        this.lostDao = lostDao;
    }

    @GetMapping
    public List<LostItem> list() throws SQLException {
        return lostDao.findAll();
    }

    @GetMapping("/{id}")
    public LostItem get(@PathVariable int id) throws SQLException {
        return lostDao.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LostItem create(@RequestBody LostItem item) throws SQLException {
        validate(item);
        item.setLostId(0);
        item.setStatus(item.getStatus() == null ? ItemStatus.OPEN : item.getStatus());
        lostDao.insert(item);
        return item;
    }

    @PutMapping("/{id}")
    public LostItem update(@PathVariable int id, @RequestBody LostItem item) throws SQLException {
        LostItem existing = lostDao.findById(id).orElseThrow(() -> notFound(id));
        validate(item);
        item.setLostId(id);
        if (item.getStatus() == null) {
            item.setStatus(existing.getStatus());
        }
        lostDao.update(item);
        return item;
    }

    @PatchMapping("/{id}/status")
    public LostItem updateStatus(@PathVariable int id, @RequestBody StatusRequest request)
            throws SQLException {
        LostItem existing = lostDao.findById(id).orElseThrow(() -> notFound(id));
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        existing.setStatus(request.status());
        lostDao.updateStatus(id, request.status());
        return existing;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) throws SQLException {
        if (!lostDao.delete(id)) {
            throw notFound(id);
        }
    }

    private void validate(LostItem item) {
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (isBlank(item.getItemName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item name is required");
        }
        if (isBlank(item.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        if (item.getDateLost() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date lost is required");
        }
        if (isBlank(item.getContactName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contact name is required");
        }
        if (isBlank(item.getContactInfo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contact info is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseStatusException notFound(int id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Lost item #" + id + " does not exist");
    }

    /** Request body for PATCH /api/lost/{id}/status. */
    public record StatusRequest(ItemStatus status) {
    }
}
