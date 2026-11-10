package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Approves a PENDING claim: it is settled, its due fixed at this moment at its live figure (a
 * PENDING claim is bound by no statement) with the late charge it has gained by the sheet's today.
 * Only a steward may approve it, and not the user who opened it.
 */
public class ApproveClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, HttpServletRequest request)
            throws InvalidClaimStatusException, ApprovalException {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.SETTLED);
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new ApprovalException("Only a steward may approve a claim: " + user + " is a clerk");
        }
        if (user.equals(claim.getOpenedBy())) {
            throw new ApprovalException("A claim cannot be approved by " + user + ", who opened it");
        }
        Person person = people.find(sheet.id(), claim.getPersonId());
        // Read while the claim is still PENDING: its live figure and its late charge.
        claim.approve(ClaimFigure.withLate(claim, rates.findAll(sheet.id()), person.gradeOn(claim.getDate()),
                settings.find(sheet.id()), sheet.today()));
    }
}
