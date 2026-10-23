package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims, newest first, each with its status log and its comments and its lines and whether it is deferred on the sheet's today and the settle batch that settled it and its review day and its shares and the user assigned to review it; given
 *  an as-of date, as they stood at the end of it, with their logs and comments then and deferred on it. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, ClaimRepository repository,
            ClaimLogRepository logs, ClaimCommentRepository comments, ClaimLineRepository claimLines, ClaimShareRepository claimShares, SheetRepository sheets, SettleBatchRepository batches,
            ObjectResponse<List<ClaimResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        Map<Long, List<String>> lines = logs.lines(id, end);
        Map<Long, List<String>> said = comments.lines(id, end);
        Map<Long, List<String>> lined = claimLines.lines(id, end);
        Map<Long, List<ClaimShareRepository.Share>> shared = claimShares.shares(id, end);
        Map<Long, Integer> settledBy = batches.byClaim(id);
        Map<Long, LocalDate> reviewDays = repository.reviewDays(id);
        Map<Long, String> reviewers = repository.reviewers(id);
        var today = end != null ? end : sheets.today(id);
        List<Claim> claims = end != null ? repository.listAsOf(id, end) : repository.list(id);
        response.send(claims.stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.id(), List.of()), today,
                        said.getOrDefault(c.id(), List.of()), batchOf(c, settledBy),
                        lined.getOrDefault(c.id(), List.of()), reviewDays.get(c.id()))
                        .withShares(shared.getOrDefault(c.id(), List.of()))
                        .withReviewer(reviewers.get(c.id()))).toList());
    }

    /** The batch that settled the claim, shown while it is PENDING or SETTLED; null otherwise. */
    private static Integer batchOf(Claim c, Map<Long, Integer> settledBy) {
        return c.status() == ClaimStatus.PENDING || c.status() == ClaimStatus.SETTLED ? settledBy.get(c.id()) : null;
    }
}
