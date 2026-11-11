package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's claims, newest first, with their shares, and the tags ever used on it; read as of a date, as they stood then. */
public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository, ClaimLogRepository log,
            ClaimCommentRepository comments, ClaimLineRepository lines,
            HttpServletRequest request, ObjectResponse<ClaimsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        Map<Long, List<String>> logs = asOf == null ? log.findAll(sheet.id()) : log.findAllOn(sheet.id(), asOf);
        Map<Long, List<String>> notes = comments.findAll(sheet.id(), asOf);
        Map<Long, List<String>> items = lines.findAll(sheet.id(), asOf);
        Map<Long, List<ClaimShare>> held = repository.shares(sheet.id(), asOf);
        List<Claim> claims = asOf == null ? repository.findAll(sheet.id()) : repository.findAllAsOf(sheet.id(), asOf);
        response.send(new ClaimsResponse(claims.stream()
                .map((claim) -> ClaimResponse.from(claim, logs.getOrDefault(claim.getId(), List.of()),
                        notes.getOrDefault(claim.getId(), List.of()),
                        items.getOrDefault(claim.getId(), List.of()),
                        held.getOrDefault(claim.getId(), List.of()))).toList(),
                repository.tagsUsed(sheet.id(), asOf),
                Objects.toString(repository.lastBatch(sheet.id(), asOf), "")));
    }
}
