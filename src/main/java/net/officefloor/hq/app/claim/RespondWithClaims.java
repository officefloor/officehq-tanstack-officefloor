package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claims, newest first, each with its status log. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository claims,
            ClaimLogRepository log, SheetRepository sheets, ObjectResponse<List<ClaimResponse>> response) {
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(sheetId.trim());
        LocalDate today = sheets.today(sheetId.trim());
        Map<Long, String> locks = claims.locks(sheetId.trim());
        response.send(claims.list(sheetId.trim()).stream()
                .map((c) -> ClaimResponse.from(c, claims, logs.getOrDefault(c.id(), List.of()), today, locks.get(c.id()))).toList());
    }
}
