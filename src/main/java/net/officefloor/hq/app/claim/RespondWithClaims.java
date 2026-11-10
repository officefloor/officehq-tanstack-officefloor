package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's claims, newest first, and the tags ever used on it; read as of a date, as they stood then. */
public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository, ClaimLogRepository log,
            HttpServletRequest request, ObjectResponse<ClaimsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        Map<Long, List<String>> logs = asOf == null ? log.findAll(sheet.id()) : log.findAllOn(sheet.id(), asOf);
        List<Claim> claims = asOf == null ? repository.findAll(sheet.id()) : repository.findAllAsOf(sheet.id(), asOf);
        response.send(new ClaimsResponse(claims.stream()
                .map((claim) -> ClaimResponse.from(claim, logs.getOrDefault(claim.getId(), List.of()))).toList(),
                repository.tagsUsed(sheet.id(), asOf)));
    }
}
