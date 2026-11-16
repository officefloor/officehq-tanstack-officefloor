package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims, newest first, each with its status log. Read as of a date (see AsOf), each as it stood then:
 *  one added later is left out; its amount, date and note are those it had before any later edit; its status, and its log, are those its log shows then; it has the number and opener
 *  only once opened by then, its settled due only while SETTLED then, and is bound only when its person's last
 *  statement was taken while the sheet's today was on or before the date; the date stands in for today. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, ClaimRepository claims,
            ClaimLogRepository log, SheetRepository sheets, StatementRepository statements,
            ObjectResponse<List<ClaimResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(sheet);
        LocalDate today = date == null ? sheets.today(sheet) : date;
        Map<Long, String> locks = claims.locks(sheet);
        Set<Long> late = claims.lateEntries(sheet);
        Map<Long, LocalDate> reminded = claims.reminders(sheet, date);
        Map<Long, Claim> edited = date == null ? Map.of() : claims.editedAfter(sheet, date);
        List<ClaimResponse> out = new ArrayList<>();
        for (Claim c : claims.list(sheet)) {
            List<ClaimLogLine> lines = logs.getOrDefault(c.id(), List.of());
            Claim shown = c;
            if (date != null) {
                lines = lines.stream().filter((l) -> !l.date().isAfter(date)).toList();
                if (lines.isEmpty() && !logs.getOrDefault(c.id(), List.of()).isEmpty()) {
                    continue;
                }
                shown = asItStood(c, edited.get(c.id()), lines, date, statements);
            }
            out.add(ClaimResponse.from(shown, claims, lines, today, date == null ? locks.get(c.id()) : null,
                    late.contains(c.id()), reminded.get(c.id())));
        }
        response.send(out);
    }

    /** The claim as it stood at the end of the date, its log (oldest first) cut there, with the values it had before
     *  its first edit after the date (null: none). */
    private static Claim asItStood(Claim c, Claim before, List<ClaimLogLine> lines, LocalDate date, StatementRepository statements) {
        ClaimStatus status = lines.isEmpty() ? c.status() : lines.get(lines.size() - 1).status();
        boolean opened = lines.isEmpty() || lines.stream().anyMatch((l) -> l.status() == ClaimStatus.OPEN);
        Statement last = c.stated() ? statements.last(c.sheetId(), c.personId()) : null;
        boolean stated = last != null && AsOf.counts(last.madeOn(), date);
        Claim values = before == null ? c : before;
        return new Claim(c.id(), c.sheetId(), c.personId(), values.amount(), values.date(), values.note(), status,
                opened ? c.number() : null, status == ClaimStatus.SETTLED ? c.due() : null, opened ? c.openedBy() : null,
                stated, stated ? c.statedDue() : null, values.corrects(), c.dueBy(), c.deferredUntil(), c.proofs(),
                c.splitFrom());
    }
}
