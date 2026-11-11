package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Judges a part-settle on the original claim, before it is split: only a role the permission matrix
 * allows to settle may, not the user who opened the claim or last edited it, and not while its due
 * now is above the proof threshold without the proofs it needs. Refuses before anything is written.
 */
public class JudgePartSettle {
    public void judge(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, HttpServletRequest request)
            throws InvalidClaimStatusException, SelfSettleException {
        String user = Audit.user(request);
        Settings settings = settingsStore.find(sheet.id());
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.allows("settle", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not settle a claim");
        }
        String grade = people.find(sheet.id(), claim.getPersonId()).gradeOn(claim.getDate());
        SettleClaim.judge(claim, user, SettleClaim.dueNow(sheet, claim, grade, rates, settings), grade, settings);
    }
}
