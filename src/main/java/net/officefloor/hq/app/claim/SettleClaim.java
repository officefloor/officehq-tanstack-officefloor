package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
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
 * Settles the claim and fixes its due: its amount times its person's grade's rate in force on the
 * claim's date, divided by 100 and rounded half to even to two decimals. With no such rate the due is
 * not fixed. A claim a statement bound is settled at its bound figure. The user who opened the claim may not settle it.
 * A clerk (any user not made a steward) may settle it only when its due is at or below the sheet's
 * clerk limit; a due of n/a is within any limit.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settingsStore, HttpServletRequest request)
            throws InvalidClaimStatusException, SelfSettleException, ClerkLimitException {
        claim.moveTo(ClaimStatus.SETTLED);
        String user = Audit.user(request);
        if (user.equals(claim.getOpenedBy())) throw new SelfSettleException(user);
        Person person = people.find(sheet.id(), claim.getPersonId());
        claim.setSettledDue(claim.isStated() ? claim.getStatedDue()
                : ClaimFigure.live(claim.getAmount(), rates.findAll(sheet.id()), person.getGrade(), claim.getDate()));
        Settings settings = settingsStore.find(sheet.id());
        if (!settings.withinClerkLimit(claim.getSettledDue()) && Role.CLERK.equals(roles.roleOf(sheet.id(), user))) {
            throw new ClerkLimitException(user, claim.getSettledDue(), settings.clerkLimit());
        }
    }
}
