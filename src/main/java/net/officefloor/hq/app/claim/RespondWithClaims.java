package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimStore store, ClaimLogStore log, SheetStore sheets,
            ObjectResponse<List<ClaimResponse>> response) {
        List<Claim> claims = store.list(sheetId.trim());
        // Only the dates the claims round by are read here, so no policy date is needed.
        Rounding rounding = new Rounding(null, claims);
        LocalDate today = sheets.today(sheetId.trim());
        Map<Long, List<String>> lines = log.lines(sheetId.trim());
        response.send(claims.stream()
                .map(c -> ClaimResponse.from(c, rounding).withLog(lines.getOrDefault(c.getId(), List.of())).on(today)).toList());
    }
}
