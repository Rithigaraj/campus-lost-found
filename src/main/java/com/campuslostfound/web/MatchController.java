package com.campuslostfound.web;

import com.campuslostfound.dao.FoundItemDAO;
import com.campuslostfound.dao.LostItemDAO;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.service.MatchResult;
import com.campuslostfound.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * REST API for the smart-matching engine.
 *
 * GET  /api/matches?scope=all|lost|found&amp;id={reportId}  list possible matches
 * POST /api/matches/confirm  mark a lost/found pair as MATCHED
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchingService matching;
    private final LostItemDAO lostDao;
    private final FoundItemDAO foundDao;

    public MatchController(MatchingService matching, LostItemDAO lostDao, FoundItemDAO foundDao) {
        this.matching = matching;
        this.lostDao = lostDao;
        this.foundDao = foundDao;
    }

    @GetMapping
    public List<MatchResult> find(@RequestParam(defaultValue = "all") String scope,
                                  @RequestParam(required = false) Integer id)
            throws SQLException {
        return switch (scope) {
            case "all" -> matching.matchAllOpenReports();
            case "lost" -> {
                if (id == null) {
                    throw badRequest("id is required for scope=lost");
                }
                yield matching.matchLostItem(id);
            }
            case "found" -> {
                if (id == null) {
                    throw badRequest("id is required for scope=found");
                }
                yield matching.matchFoundItem(id);
            }
            default -> throw badRequest("scope must be one of: all, lost, found");
        };
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody ConfirmRequest request) throws SQLException {
        if (lostDao.findById(request.lostId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Lost item #" + request.lostId() + " does not exist");
        }
        if (foundDao.findById(request.foundId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Found item #" + request.foundId() + " does not exist");
        }
        lostDao.updateStatus(request.lostId(), ItemStatus.MATCHED);
        foundDao.updateStatus(request.foundId(), ItemStatus.MATCHED);
        return Map.of(
                "lostId", request.lostId(),
                "foundId", request.foundId(),
                "status", ItemStatus.MATCHED.name());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /** Request body for POST /api/matches/confirm. */
    public record ConfirmRequest(int lostId, int foundId) {
    }
}
