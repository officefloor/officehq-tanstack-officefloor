package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.setting.UserRole;
import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Moves the claim to SETTLED, refusing it when its status does not allow that, and fixes its due at the
 *  rate its person's grade has in force on the claim's date: amount × rate / 100, rounded half up and raised to the sheet's floor (none when no rate applies),
 *  plus, when it is overdue (its date plus the sheet's terms is before the sheet's today), the sheet's late percent of that floored
 *  due for every full 30 days it is overdue, rounded the same way.
 *  The user who opened the claim (arriving as X-User) cannot settle it. A clerk (any user not made a steward)
 *  cannot settle a claim whose due is above the sheet's clerk limit (no limit when none is set; a claim with no due
 *  is within it); a steward settles any. */
public class SettleClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, PersonRepository people,
            RateRepository rates, SettingRepository settings, UserRoleRepository roles, SheetRepository sheets)
            throws InvalidClaimStatusException, SelfSettleException, ClerkLimitException {
        String by = user == null ? "" : user.trim();
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by)) {
            throw new SelfSettleException("The user who opened a claim cannot settle it.");
        }
        ClaimTransition.check(claim.getStatus(), ClaimStatus.SETTLED);
        Person person = people.findById(claim.getPersonId()).orElseThrow();
        BigDecimal due = rates.findInForce(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate())
                .map((r) -> claim.getAmount().multiply(r.getRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(null);
        due = settings.floored(claim.getSheetId(), due);
        if (due != null) {
            LocalDate dueBy = claim.getDate().plusDays(settings.terms(claim.getSheetId()));
            LocalDate today = sheets.findById(claim.getSheetId()).orElseThrow().getToday();
            long spans = dueBy.isBefore(today) ? ChronoUnit.DAYS.between(dueBy, today) / 30 : 0;
            if (spans > 0) {
                due = due.add(due.multiply(settings.latePct(claim.getSheetId())).multiply(BigDecimal.valueOf(spans))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }
        if (due != null && UserRole.CLERK.equals(roles.roleOf(claim.getSheetId(), by))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId()).orElse(null);
            if (limit != null && due.compareTo(limit) > 0) {
                throw new ClerkLimitException("A clerk cannot settle a claim whose due is above the clerk limit of "
                        + limit.setScale(2).toPlainString() + ".");
            }
        }
        ClaimTransition.to(claim, ClaimStatus.SETTLED);
        claim.setSettledDue(due);
    }
}
