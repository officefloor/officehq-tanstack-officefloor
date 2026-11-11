package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Approves a PENDING claim: it is settled, its due fixed at this moment at its rate figure (the
 * figure a statement bound it to, else its live figure) with the late charge it has gained by the sheet's today.
 * Only a steward may approve it, and not the user who opened it or last edited it; and not while it has no proofs
 * when its due is above the proof threshold in force on its date.
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
        if (claim.isOwnedBy(user)) {
            throw new ApprovalException("A claim cannot be approved by " + user + ", who last edited it");
        }
        Person person = people.find(sheet.id(), claim.getPersonId());
        Settings sheetSettings = settings.find(sheet.id());
        // Read while the claim is still PENDING: its rate figure and its late charge.
        java.math.BigDecimal due = ClaimFigure.withLate(claim, rates.findAll(sheet.id()), person.gradeOn(claim.getDate()),
                sheetSettings, sheet.today());
        if (sheetSettings.needsProof(due, person.gradeOn(claim.getDate()), claim.getDate(), claim)) {
            throw new InvalidClaimStatusException(ProofRequired.MESSAGE);
        }
        claim.approve(due);
    }
}
