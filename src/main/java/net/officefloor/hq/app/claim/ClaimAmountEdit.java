package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;

/** Edit a DRAFT claim's amount only, by the claim editor's rules (its date, note, reference and tags stay as they
 *  are): CLAIM_EDITED when the amount changes; an unchanged amount writes nothing. */
public final class ClaimAmountEdit {
    private ClaimAmountEdit() {
    }

    public static Claim apply(String id, long cid, BigDecimal amount, String by, ClaimRepository repository,
            PeriodRepository periods, Audit audit, Rollup rollup) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be edited");
        }
        if (new BigDecimal(found.amount()).compareTo(amount) == 0) {
            return found;
        }
        String holder = repository.lockedBy(id, cid);
        if (holder != null && !holder.equals(by)) {
            throw new BadRequestException("the claim is being edited by " + holder);
        }
        BigDecimal lines = repository.linesTotal(id, cid);
        if (lines != null && lines.compareTo(amount) != 0) {
            throw new BadRequestException("a claim with lines has their total, " + ClaimRepository.money(lines) + ", as its amount");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.update(id, cid, amount, LocalDate.parse(found.date()), LocalDate.parse(found.dueBy()), found.note(),
                found.corrects(), found.ref(), found.tags())) {
            throw new BadRequestException("only a DRAFT claim can be edited");
        }
        repository.editedBy(id, cid, by);
        Claim saved = repository.find(id, cid);
        audit.record("CLAIM_EDITED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + saved.amount() + " date=" + saved.date() + Claim.auditNumber(saved.number()));
        rollup.record(id, by, before);
        return saved;
    }
}
