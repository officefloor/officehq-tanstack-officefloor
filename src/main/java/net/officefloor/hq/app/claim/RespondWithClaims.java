package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claims, newest first, each with its status log. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository, ClaimLogRepository logs,
            ObjectResponse<List<ClaimResponse>> response) {
        String id = sheetId.trim();
        Map<Long, List<String>> lines = logs.lines(id);
        response.send(repository.list(id).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.id(), List.of()))).toList());
    }
}
