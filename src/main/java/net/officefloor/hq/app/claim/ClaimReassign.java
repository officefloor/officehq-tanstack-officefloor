package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;

/** Moving one claim to another person, shared by a single move and a bulk one (see {@link SaveReassign}). */
final class ClaimReassign {
    private ClaimReassign() {
    }

    /** Move the claim by the rules of {@link SaveReassign}, answering it as moved. A numbered claim takes the next number in the
     *  sequence it would be numbered in now (its old one is never used again); a draft stays unnumbered. A bulk move ({@code bulk}) also lets a claim
     *  "move" to the person it already belongs to: it is written (losing any statement figure) and audited as a
     *  move like any other. */
    public static ClaimResponse move(ReassignInput input, Claim loaded, ClaimRepository repository,
            PersonRepository people, RoleRepository roles, SettingRepository settings, boolean bulk)
            throws ClaimStatusNotAllowedException, PersonNotFoundException, RetiredPersonException {
        if (!settings.permissions(loaded.sheetId()).allows("reassign", roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Your role may not move a claim to another person on this sheet.");
        }
        if (!loaded.status().unsettled() && loaded.status() != ClaimStatus.DRAFT) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot move; only a DRAFT, OPEN or PENDING one can.");
        }
        if (loaded.corrects() != null) {
            throw new ClaimStatusNotAllowedException("A correction cannot move to another person.");
        }
        Person to = people.find(loaded.sheetId(), input.personId());
        if (to == null) {
            throw new PersonNotFoundException(String.valueOf(input.personId()));
        }
        if (!bulk && to.id() == loaded.personId()) {
            throw new ClaimStatusNotAllowedException("The claim is already " + to.name() + "'s.");
        }
        RetiredPersonException.refuse(to, loaded.date());
        String number = loaded.number() != null ? repository.nextNumber(loaded.sheetId(), loaded.dueBy()) : null;
        if (!repository.reassign(loaded, to.id(), number)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        return ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), to.id(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), number, loaded.settledDue(),
                loaded.openedBy(), false, null, loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(), loaded.ref(), loaded.tags()));
    }

    /** The CLAIM_REASSIGNED line: the claim like every claim line (with its new person), then the person it moved
     *  from and to. */
    public static void audit(ReassignInput input, Claim loaded, ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var from = people.find(loaded.sheetId(), loaded.personId());
        var to = people.find(loaded.sheetId(), saved.personId());
        audit.record("CLAIM_REASSIGNED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + to.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " from=" + from.name() + " to=" + to.name());
    }
}
