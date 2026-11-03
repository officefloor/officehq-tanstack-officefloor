package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A claim's move to another person, shared by the single and the bulk reassign: only a role the sheet's
 *  permission matrix allows (by default a steward) may; only a DRAFT, OPEN or PENDING claim, outside a closed
 *  period, and not to a person retired from before its date. The claim keeps its date and takes the next number in
 *  the sequence it would be numbered in now (the sheet's, or its period's), its old number never used again (a
 *  DRAFT, having none, gets none); it loses any figure a statement bound it to and is listed as its new person's
 *  newest claim. Every check is made before anything is written, so a refusal writes nothing to the store or the audit. */
final class ClaimReassign {
    private ClaimReassign() {
    }

    /** @param ownAllowed whether a claim may be "moved" to the person it already is with (the bulk move
     *                    re-lists it as that person's newest); the single move refuses it */
    static Claim apply(String id, Long claimId, Long personId, boolean ownAllowed, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, PersonRepository people, Audit audit, Rollup rollup)
            throws BadRequestException {
        if (claimId == null) {
            throw new BadRequestException("no such claim");
        }
        if (personId == null) {
            throw new BadRequestException("choose the person to move the claim to");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        if (!settings.allows(id, "reassign", by)) {
            throw new BadRequestException("a " + settings.role(id, by) + " cannot reassign a claim");
        }
        Claim found = repository.find(id, claimId);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT") && !found.open()) {
            throw new BadRequestException("a " + found.status() + " claim cannot be reassigned");
        }
        long to = personId;
        if (!repository.personExists(id, to)) {
            throw new BadRequestException("no such person");
        }
        if (to == found.personId() && !ownAllowed) {
            throw new BadRequestException("the claim is already that person's");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        people.claimAllowed(id, to, LocalDate.parse(found.date()));
        String fromName = repository.personName(id, found.personId());
        String toName = repository.personName(id, to);
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.reassign(id, found.id(), to)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be reassigned");
        }
        String number = found.number();
        if (!found.status().equals("DRAFT")) {
            number = repository.numbersPerPeriod(id)
                    ? repository.numberInPeriod(id, found.id(), LocalDate.parse(found.dueBy()))
                    : repository.number(id, found.id(), LocalDate.parse(found.dueBy()));
        }
        Claim moved = new Claim(found.id(), to, found.amount(), found.date(), found.note(), found.status(),
                number, found.due(), found.openedBy(), false, null, found.corrects(), found.dueBy(), false,
                found.deferredUntil(), found.deferred(), found.proofs(), found.splitFrom(), found.lateEntry(), found.reminded(), found.ref(), found.tags(), found.voidReason());
        audit.record("CLAIM_REASSIGNED sheet=" + id + " by=" + by + " person=" + toName + " amount=" + moved.amount()
                + " date=" + moved.date() + Claim.auditNumber(moved.number()) + " from=" + fromName + " to=" + toName);
        rollup.record(id, by, before);
        return moved;
    }
}
