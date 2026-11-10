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
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim and fixes its due: its amount times the rate of its person's grade in force on its date, in force on the
 * claim's date, divided by 100 and rounded half to even to two decimals, at least the sheet's floor. With no such rate the due is
 * not fixed. A claim a statement bound is settled at its bound figure. The late charge it has gained
 * by the sheet's today is frozen in on top. The user who opened the claim may not settle it.
 * A clerk (any user not made a steward) may settle it only when its due is at or below the sheet's
 * clerk limit; a due of n/a is within any limit.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, HttpServletRequest request)
            throws InvalidClaimStatusException, SelfSettleException, ClerkLimitException {
        Person person = people.find(sheet.id(), claim.getPersonId());
        Settings settings = settingsStore.find(sheet.id());
        BigDecimal figure = claim.isStated() ? claim.getStatedDue()
                : ClaimFigure.live(claim.getAmount(), rates.findAll(sheet.id()), person.gradeOn(claim.getDate()), claim.getDate(),
                        settings.floor());
        // The late charge is read while the claim is still OPEN.
        BigDecimal late = ClaimFigure.late(claim, figure, settings, sheet.today());
        claim.moveTo(ClaimStatus.SETTLED);
        String user = Audit.user(request);
        if (user.equals(claim.getOpenedBy())) throw new SelfSettleException(user);
        claim.setSettledDue(figure == null ? null : figure.add(late));
        if (!settings.withinClerkLimit(claim.getSettledDue()) && Role.CLERK.equals(roles.roleOf(sheet.id(), user))) {
            throw new ClerkLimitException(user, claim.getSettledDue(), settings.clerkLimit());
        }
    }
}
