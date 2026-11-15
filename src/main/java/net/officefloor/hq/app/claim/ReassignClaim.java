package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A role the permission matrix lets reassign (by default a steward) moves a DRAFT, OPEN or PENDING claim to another person on the sheet ({@code to}, their
 *  id): it keeps its date, takes the next number in the sequence it would be numbered in (see
 *  {@link ClaimNumbers}; its old number is never used again; a draft gets none), and loses any figure a statement bound it to (live again until
 *  its new person's next statement). Refused for anyone else, for a SETTLED or VOID claim, for a claim
 *  due by a day in a closed period, for a claim dated after the new person's retirement, and when
 *  {@code to} is not another person on the sheet. */
public class ReassignClaim {
    public void move(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "to", required = false) Long to, @Val Claim claim,
            PersonRepository people, RoleRepository roles, PeriodRepository periods,
            SettingRepository settings, ClaimRepository claims, SheetRepository sheets,
            Out<Reassignment> reassignment) throws ClaimStatusException, PeriodClosedException {
        Person target = to == null ? null : people.find(claim.getSheetId(), to);
        if (target != null && target.getId() == claim.getPersonId()) {
            throw new ClaimStatusException("A claim can only be reassigned to another person on the sheet");
        }
        reassignment.set(reassign(by == null ? "" : by, claim, target, people, roles, periods, settings,
                claims, sheets));
    }

    /** Move the claim to {@code target} by the rules of reassigning one (its person, and a numbered
     *  claim's number, changed in place; nothing else is saved): refused for a role that may not reassign, a SETTLED or VOID claim, a claim
     *  due by a day in a closed period, no target, or a target retired before the claim's date. */
    static Reassignment reassign(String user, Claim claim, Person target, PersonRepository people,
            RoleRepository roles, PeriodRepository periods, SettingRepository settings,
            ClaimRepository claims, SheetRepository sheets) throws ClaimStatusException, PeriodClosedException {
        if (!Permissions.allows(claim.getSheetId(), user, Permissions.REASSIGN, roles, settings)) {
            throw new ClaimStatusException("Your role may not reassign a claim");
        }
        String status = claim.getStatus();
        if (!ClaimStatus.DRAFT.name().equals(status) && !ClaimStatus.isOpen(status)) {
            throw new ClaimStatusException("A " + status + " claim cannot be reassigned");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (target == null) {
            throw new ClaimStatusException("A claim can only be reassigned to another person on the sheet");
        }
        if (target.retiredBefore(java.time.LocalDate.parse(claim.getDate()))) {
            throw new ClaimStatusException("A claim cannot be reassigned to a person retired before its date");
        }
        Person from = people.find(claim.getSheetId(), claim.getPersonId());
        claim.setPersonId(target.getId());
        claim.bind(false, null);
        if (claim.getNumber() != null) {
            claim.setNumber(ClaimNumbers.next(claim, claims, periods, settings, sheets));
        }
        return new Reassignment(from == null ? "" : from.getName(), target.getName());
    }
}
