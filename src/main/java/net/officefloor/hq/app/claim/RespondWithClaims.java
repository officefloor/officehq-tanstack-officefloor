package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, Claims claims, Figures figures, ClaimLog log,
            ObjectResponse<List<ClaimResponse>> response) {
        Map<Long, List<String>> lines = log.onSheet(sheetId);
        response.send(claims.onSheet(sheetId).stream()
                .map(c -> ClaimResponse.from(c, figures, lines.getOrDefault(c.getId(), List.of()))).toList());
    }
}
