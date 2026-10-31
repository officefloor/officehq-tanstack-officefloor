package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Claims claims, Figures figures,
            ClaimLog log, ClaimComments comments, AsOf asOfSheet, ObjectResponse<List<ClaimResponse>> response)
            throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, () -> {
            Map<Long, List<String>> lines = log.onSheet(sheetId);
            Map<Long, List<String>> said = comments.onSheet(sheetId);
            return claims.onSheet(sheetId).stream()
                    .map(c -> ClaimResponse.from(c, figures, lines.getOrDefault(c.getId(), List.of()),
                            said.getOrDefault(c.getId(), List.of()))).toList();
        }));
    }
}
