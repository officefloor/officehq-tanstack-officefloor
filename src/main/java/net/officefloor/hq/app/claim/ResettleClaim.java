package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-settles a SETTLED claim: its due is fixed again at the figure it would have today as an OPEN
 * claim bound by no statement: its live rate figure (today's rates and grades) with the late
 * charge it would have gained on it by the sheet's today, then at least the floor on its date. Only a steward may re-settle a claim.
 */
public class ResettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, HttpServletRequest request,
            Out<Resettlement> resettlement) throws InvalidClaimStatusException, ResettleException {
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new InvalidClaimStatusException("Only a SETTLED claim can be re-settled: this one is " + claim.getStatus());
        }
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new ResettleException("Only a steward may re-settle a claim: " + user + " is a clerk");
        }
        Person person = people.find(sheet.id(), claim.getPersonId());
        Settings settings = settingsStore.find(sheet.id());
        String grade = person.gradeOn(claim.getDate());
        BigDecimal figure = ClaimFigure.live(claim, rates.findAll(sheet.id()), grade, settings, sheet.today());
        BigDecimal due = ClaimFigure.floored(claim, figure,
                ClaimFigure.lateAsOpen(claim, figure, grade, settings, sheet.today()), settings);
        BigDecimal old = claim.getSettledDue();
        claim.setSettledDue(due);
        resettlement.set(new Resettlement(old, due));
    }
}
