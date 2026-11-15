package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A role the permission matrix lets reassign (by default a steward) moves a DRAFT, OPEN or PENDING claim to another person on the sheet ({@code to}, their
 *  id): it keeps its number and date and loses any figure a statement bound it to (live again until
 *  its new person's next statement). Refused for anyone else, for a SETTLED or VOID claim, for a claim
 *  due by a day in a closed period, and when {@code to} is not another person on the sheet. */
public class ReassignClaim {
    public void move(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "to", required = false) Long to, @Val Claim claim,
            PersonRepository people, RoleRepository roles, PeriodRepository periods,
            SettingRepository settings, Out<Reassignment> reassignment) throws ClaimStatusException, PeriodClosedException {
        String user = by == null ? "" : by;
        if (!Permissions.allows(claim.getSheetId(), user, Permissions.REASSIGN, roles, settings)) {
            throw new ClaimStatusException("Your role may not reassign a claim");
        }
        String status = claim.getStatus();
        if (!ClaimStatus.DRAFT.name().equals(status) && !ClaimStatus.isOpen(status)) {
            throw new ClaimStatusException("A " + status + " claim cannot be reassigned");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        Person from = people.find(claim.getSheetId(), claim.getPersonId());
        Person target = to == null ? null : people.find(claim.getSheetId(), to);
        if (target == null || target.getId() == claim.getPersonId()) {
            throw new ClaimStatusException("A claim can only be reassigned to another person on the sheet");
        }
        claim.setPersonId(target.getId());
        claim.bind(false, null);
        reassignment.set(new Reassignment(from == null ? "" : from.getName(), target.getName()));
    }
}
