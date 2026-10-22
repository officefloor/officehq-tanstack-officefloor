package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims, newest first, each with its status log and its comments and whether it is deferred on the sheet's today; given
 *  an as-of date, as they stood at the end of it, with their logs and comments then and deferred on it. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, ClaimRepository repository,
            ClaimLogRepository logs, ClaimCommentRepository comments, SheetRepository sheets,
            ObjectResponse<List<ClaimResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        Map<Long, List<String>> lines = logs.lines(id, end);
        Map<Long, List<String>> said = comments.lines(id, end);
        var today = end != null ? end : sheets.today(id);
        List<Claim> claims = end != null ? repository.listAsOf(id, end) : repository.list(id);
        response.send(claims.stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.id(), List.of()), today,
                        said.getOrDefault(c.id(), List.of()))).toList());
    }
}
