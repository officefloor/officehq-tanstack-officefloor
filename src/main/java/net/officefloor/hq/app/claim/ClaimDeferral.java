package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;

/** Defer an OPEN claim (not in a closed period) until a date after the sheet's today: until the sheet's today
 *  reaches it, the claim counts in no total, is not overdue, cannot be settled and a statement leaves it
 *  unbound. Every check is made before anything is written, so a refusal writes nothing to the store or the audit. */
final class ClaimDeferral {
    private ClaimDeferral() {
    }

    static Claim apply(String id, long cid, String until, String by, ClaimRepository repository, SheetRepository sheets,
            PeriodRepository periods, Audit audit, Rollup rollup) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("OPEN")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be deferred");
        }
        String raw = until == null ? "" : until.trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("give the date to defer the claim until");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("the date to defer until must be yyyy-mm-dd");
        }
        if (!date.isAfter(sheets.today(id))) {
            throw new BadRequestException("a claim can be deferred only to a date after today");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.date()));
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.defer(id, cid, date)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be deferred");
        }
        audit.record("CLAIM_DEFERRED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number())
                + " until=" + date);
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}
