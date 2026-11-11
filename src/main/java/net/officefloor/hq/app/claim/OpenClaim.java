package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Opens the claim, recording the signed-in user who opened it. Only a role the permission matrix allows may open. A
 * claim whose due is above the note threshold in force on its date is refused while its note is empty. A claim
 * whose due is above the review threshold in force on its date goes to REVIEW instead, still recording who opened it.
 */
public class OpenClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, SettingsRepository settings,
            PersonRepository people, RateRepository rates, HttpServletRequest request) throws InvalidClaimStatusException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.find(sheet.id()).allows("open", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not open a claim");
        }
        if (flagRefuses(sheet, claim, people, role)) {
            throw new InvalidClaimStatusException("Only a steward may open a claim of a flagged person");
        }
        if (needsNote(sheet, claim, people, rates, settings)) throw new InvalidClaimStatusException(NoteRequired.MESSAGE);
        claim.moveTo(opensTo(sheet, claim, people, rates, settings));
        claim.setOpenedBy(user);
    }

    /**
     * The status a DRAFT claim moves to when it is opened: REVIEW when its due, as it stands before it is
     * opened, is above the review threshold in force on its date, else OPEN. Any other claim cannot be opened.
     */
    static ClaimStatus opensTo(Sheet sheet, Claim claim, PersonRepository people, RateRepository rates,
            SettingsRepository settings) throws InvalidClaimStatusException {
        if (claim.getStatus() != ClaimStatus.DRAFT) throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.OPEN);
        return settings.find(sheet.id()).needsReview(LogClaimStatus.due(sheet, claim, people, rates, settings),
                claim.getDate()) ? ClaimStatus.REVIEW : ClaimStatus.OPEN;
    }

    /** Whether the claim's person is flagged on the sheet's today and {@code role} is not a steward's. */
    static boolean flagRefuses(Sheet sheet, Claim claim, PersonRepository people, String role) {
        var person = people.find(sheet.id(), claim.getPersonId());
        return person != null && person.flagRefuses(role, sheet.today());
    }

    /** Whether the claim's due, as it stands before it is opened, needs a note it does not have. */
    static boolean needsNote(Sheet sheet, Claim claim, PersonRepository people, RateRepository rates,
            SettingsRepository settings) {
        return settings.find(sheet.id()).needsNote(LogClaimStatus.due(sheet, claim, people, rates, settings),
                claim.getDate(), claim.getNote());
    }
}
