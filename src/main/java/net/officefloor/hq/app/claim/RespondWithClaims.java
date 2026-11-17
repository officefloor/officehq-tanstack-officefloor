package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.Preview;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims, newest first, each with its status log. Read as of a date (see AsOf), each as it stood then:
 *  one added later is left out; its amount, date and note are those it had before any later edit; its status, and its log, are those its log shows then; its comments are those written by then; its lines are those added by then; its proofs count leaves out the proofs added after; its shares are those made by then; its deferral is the last one made by then; it has its opener only once opened by then and its
 *  number only once settled by then (numbers are given as a claim settles; see SaveClaimStatus), its settled due only while SETTLED then, and is bound only when its person's last
 *  statement was taken while the sheet's today was on or before the date; the date stands in for today. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, ClaimRepository claims,
            ClaimLogRepository log, SheetRepository sheets, StatementRepository statements,
            ObjectResponse<List<ClaimResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(sheet);
        LocalDate today = Preview.today(sheet, date, preview, sheets);
        Map<Long, String> locks = claims.locks(sheet);
        Map<Long, Long> seqs = claims.addedSeqs(sheet);
        Set<Long> late = claims.lateEntries(sheet);
        Map<Long, LocalDate> reminded = claims.reminders(sheet, date);
        Map<Long, List<ClaimComment>> comments = claims.comments(sheet, date);
        Map<Long, Integer> batches = claims.batches(sheet, date);
        Set<Long> waived = claims.waived(sheet, date);
        Map<Long, String> reviewAssignees = claims.reviewAssignees(sheet);
        Map<Long, List<ClaimShare>> shares = claims.shares(sheet, date);
        Map<Long, List<ClaimLine>> lineItems = claims.lines(sheet, date);
        Map<Long, Claim> edited = date == null ? Map.of() : claims.editedAfter(sheet, date);
        Map<Long, Integer> proofsAfter = date == null ? Map.of() : claims.proofsAfter(sheet, date);
        Map<Long, LocalDate> deferred = date == null ? Map.of() : claims.deferredAsOf(sheet, date);
        List<ClaimResponse> out = new ArrayList<>();
        for (Claim c : claims.list(sheet)) {
            List<ClaimLogLine> lines = logs.getOrDefault(c.id(), List.of());
            Claim shown = c;
            if (date != null) {
                lines = lines.stream().filter((l) -> !l.date().isAfter(date)).toList();
                if (lines.isEmpty() && !logs.getOrDefault(c.id(), List.of()).isEmpty()) {
                    continue;
                }
                shown = asItStood(c, edited.get(c.id()), proofsAfter.getOrDefault(c.id(), 0),
                        deferred.containsKey(c.id()) ? deferred.get(c.id()) : c.deferredUntil(), lines, date, statements);
            }
            out.add(ClaimResponse.from(shown, claims, lines, today, date == null ? locks.get(c.id()) : null,
                    late.contains(c.id()), reminded.get(c.id()), comments.getOrDefault(c.id(), List.of()),
                    batches.get(c.id()), lineItems.getOrDefault(c.id(), List.of()), seqs.getOrDefault(c.id(), 0L), waived.contains(c.id()),
                    shares.getOrDefault(c.id(), List.of()), reviewAssignees.get(c.id())));
        }
        response.send(out);
    }

    /** The claim as it stood at the end of the date, its log (oldest first) cut there, with the values it had before
     *  its first edit after the date (null: none), without the proofs added after it and deferred until the date given. */
    private static Claim asItStood(Claim c, Claim before, int proofsAfter, LocalDate deferredUntil, List<ClaimLogLine> lines, LocalDate date, StatementRepository statements) {
        ClaimStatus status = lines.isEmpty() ? c.status() : lines.get(lines.size() - 1).status();
        boolean opened = lines.isEmpty() || lines.stream().anyMatch((l) -> l.status() == ClaimStatus.OPEN);
        // Opening a claim may send it to REVIEW instead of OPEN: it was opened all the same.
        boolean openedBy = opened || lines.stream().anyMatch((l) -> l.status() == ClaimStatus.REVIEW);
        boolean numbered = lines.isEmpty() || lines.stream().anyMatch((l) -> l.status() == ClaimStatus.SETTLED);
        Statement last = c.stated() ? statements.last(c.sheetId(), c.personId()) : null;
        boolean stated = last != null && AsOf.counts(last.madeOn(), date);
        Claim values = before == null ? c : before;
        return new Claim(c.id(), c.sheetId(), c.personId(), values.amount(), values.date(), values.note(), status,
                numbered ? c.number() : null, status == ClaimStatus.SETTLED ? c.due() : null, openedBy ? c.openedBy() : null,
                stated, stated ? c.statedDue() : null, values.corrects(), c.dueBy(), deferredUntil, c.proofs() - proofsAfter,
                c.splitFrom(), values.ref(), values.tags(), c.held());
    }
}
