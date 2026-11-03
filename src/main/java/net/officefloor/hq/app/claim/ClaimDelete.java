package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Delete a DRAFT claim (not in a closed period, nor held by another user's editor): it becomes DELETED, counts
 *  nowhere and can be neither edited, opened nor voided; its log gains a DELETED line and CLAIM_DELETED is written.
 *  Or restore a DELETED claim on the same sheet-today it was deleted: it is a DRAFT again, its log gains a DRAFT line
 *  and CLAIM_RESTORED is written; on a later day the restore is refused. Every check is made before anything is
 *  written, so a refusal writes nothing to the store or the audit. */
final class ClaimDelete {
    private ClaimDelete() {
    }

    static Claim delete(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT")) {
            throw new BadRequestException("only a DRAFT claim can be deleted");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        String holder = repository.lockedBy(id, cid);
        if (holder != null && !holder.equals(by)) {
            throw new BadRequestException("the claim is being edited by " + holder);
        }
        if (!repository.changeStatus(id, cid, List.of("DRAFT"), "DELETED")) {
            throw new BadRequestException("only a DRAFT claim can be deleted");
        }
        repository.unlock(id, cid, by);
        repository.log(id, cid, by);
        audit.record("CLAIM_DELETED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date());
        return repository.find(id, cid);
    }

    static Claim restore(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DELETED")) {
            throw new BadRequestException("only a DELETED claim can be restored");
        }
        if (!repository.deletedToday(id, cid)) {
            throw new BadRequestException("a claim can be restored only on the day it was deleted");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        if (!repository.changeStatus(id, cid, List.of("DELETED"), "DRAFT")) {
            throw new BadRequestException("only a DELETED claim can be restored");
        }
        repository.log(id, cid, by);
        audit.record("CLAIM_RESTORED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date());
        return repository.find(id, cid);
    }
}
