package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A steward sets a DRAFT, OPEN or PENDING claim's due-by directly: neither its due-by nor the new one may be in a
 *  closed period. A due-by set this way stays as set (a holiday added later does not move it); overdue, the late
 *  charge, grace and the claim's period follow it. Every check is made before anything is written, so a refusal writes
 *  nothing to the store or the audit. */
final class ClaimDueBySet {
    private ClaimDueBySet() {
    }

    static Claim apply(String id, long cid, String newDueBy, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can set a claim's due-by");
        }
        String status = found.status();
        if (!status.equals("DRAFT") && !status.equals("OPEN") && !status.equals("PENDING")) {
            throw new BadRequestException("a " + status + " claim's due-by cannot be set");
        }
        String raw = newDueBy == null ? "" : newDueBy.trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("give the claim's new due-by");
        }
        LocalDate dueBy;
        try {
            dueBy = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("the new due-by must be yyyy-mm-dd");
        }
        LocalDate old = LocalDate.parse(found.dueBy());
        PeriodGuard.open(periods, id, old);
        PeriodGuard.open(periods, id, dueBy);
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.setDueBy(id, cid, dueBy)) {
            throw new BadRequestException("a " + status + " claim's due-by cannot be set");
        }
        audit.record("CLAIM_DUE_BY_SET sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number())
                + " old=" + old + " new=" + dueBy);
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}
