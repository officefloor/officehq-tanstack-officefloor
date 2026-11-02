package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;

/** A claim's editor lock: opening the editor (the edit button) locks the claim for every other user until its
 *  editor is saved or cancelled, or the sheet's today changes. Only a DRAFT outside a closed period can be
 *  edited: any other claim is refused and takes no lock, as is a claim another user holds. A refusal writes nothing. */
final class ClaimLock {
    private ClaimLock() {
    }

    static Claim lock(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods)
            throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT")) {
            throw new BadRequestException("only a DRAFT claim can be edited");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.date()));
        if (!repository.lock(id, found, by)) {
            throw new BadRequestException("the claim is being edited by " + repository.lockedBy(id, cid));
        }
        return found;
    }

    static Claim unlock(String id, long cid, String by, ClaimRepository repository) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        repository.unlock(id, cid, by);
        return found;
    }
}
