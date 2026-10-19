package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every claim on the sheet, newest first, each with its status log. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository, ClaimLog log,
            ObjectResponse<List<ClaimResponse>> response) {
        var lines = log.bySheet(sheetId);
        response.send(repository.findBySheet(sheetId).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()))).toList());
    }
}
