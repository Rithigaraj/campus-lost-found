package com.campuslostfound.web;

import com.campuslostfound.dao.FoundItemDAO;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.ItemStatus;
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
 * REST API for found-item reports (FOUND_ITEMS table).
 *
 * GET    /api/found             list all reports
 * GET    /api/found/{id}        get one report
 * POST   /api/found             create a report
 * PUT    /api/found/{id}        update a report
 * PATCH  /api/found/{id}/status update only the status
 * DELETE /api/found/{id}        delete a report
 */
@RestController
@RequestMapping("/api/found")
public class FoundItemController {

    private final FoundItemDAO foundDao;

    public FoundItemController(FoundItemDAO foundDao) {
        this.foundDao = foundDao;
    }

    @GetMapping
    public List<FoundItem> list() throws SQLException {
        return foundDao.findAll();
    }

    @GetMapping("/{id}")
    public FoundItem get(@PathVariable int id) throws SQLException {
        return foundDao.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoundItem create(@RequestBody FoundItem item) throws SQLException {
        validate(item);
        item.setFoundId(0);
        item.setStatus(item.getStatus() == null ? ItemStatus.OPEN : item.getStatus());
        foundDao.insert(item);
        return item;
    }

    @PutMapping("/{id}")
    public FoundItem update(@PathVariable int id, @RequestBody FoundItem item) throws SQLException {
        FoundItem existing = foundDao.findById(id).orElseThrow(() -> notFound(id));
        validate(item);
        item.setFoundId(id);
        if (item.getStatus() == null) {
            item.setStatus(existing.getStatus());
        }
        foundDao.update(item);
        return item;
    }

    @PatchMapping("/{id}/status")
    public FoundItem updateStatus(@PathVariable int id, @RequestBody StatusRequest request)
            throws SQLException {
        FoundItem existing = foundDao.findById(id).orElseThrow(() -> notFound(id));
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        existing.setStatus(request.status());
        foundDao.updateStatus(id, request.status());
        return existing;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) throws SQLException {
        if (!foundDao.delete(id)) {
            throw notFound(id);
        }
    }

    private void validate(FoundItem item) {
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (isBlank(item.getItemName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item name is required");
        }
        if (isBlank(item.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        if (item.getDateFound() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date found is required");
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
                "Found item #" + id + " does not exist");
    }

    /** Request body for PATCH /api/found/{id}/status. */
    public record StatusRequest(ItemStatus status) {
    }
}
