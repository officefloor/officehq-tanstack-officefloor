package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;

/** Re-dating one claim, shared by a single re-date and a bulk one (see {@link SaveRedate}). */
final class ClaimRedate {
    private ClaimRedate() {
    }

    /** Re-date the claim by the rules of {@link SaveRedate}, answering it as re-dated. */
    public static ClaimResponse redate(RedateInput input, Claim loaded, ClaimRepository repository,
            PersonRepository people, RoleRepository roles)
            throws ClaimStatusNotAllowedException, RetiredPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may re-date a claim.");
        }
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be re-dated; only an OPEN or PENDING one can.");
        }
        RetiredPersonException.refuse(people.find(loaded.sheetId(), loaded.personId()), input.date());
        String number = loaded.number();
        if (!repository.redate(loaded, input.date(), number)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        return ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                input.date(), loaded.note(), loaded.status(), number, loaded.settledDue(), loaded.openedBy(),
                loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(), loaded.ref(), loaded.tags(), loaded.waived()));
    }

    /** Record the stored re-date in the audit file (see {@link AuditClaimRedated}). */
    public static void audit(RedateInput input, Claim loaded, ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("CLAIM_REDATED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " old=" + loaded.date() + " new=" + saved.date());
    }
}
