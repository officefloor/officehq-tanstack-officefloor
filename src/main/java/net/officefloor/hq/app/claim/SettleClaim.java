package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Settles a claim, fixing its due: its amount at its person's grade's rate in force on its date, rounded half to even to
 *  two decimals (a claim bound by a statement: the figure it was bound to). With no rate the due is not
 *  fixed and stays live. Refused for the user who opened it (four eyes), and for a clerk when the due
 *  is above the sheet's clerk limit (a due that is n/a is within any limit; a steward settles anything). */
public class SettleClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings)
            throws ClaimStatusException, ClaimLimitException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        ClaimStatus settled = ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.SETTLED);
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(by == null ? "" : by)) {
            throw new ClaimStatusException("The user who opened a claim cannot settle it");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.getGrade(),
                LocalDate.parse(claim.getDate()));
        // A claim bound by a statement settles at the figure it was bound to.
        String due = claim.isStated() ? claim.getStatedDue()
                : rate == null ? null : Dues.of(claim.getAmount(), rate.pct());
        if (due != null && Role.CLERK.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            if (limit != null && new BigDecimal(due).compareTo(limit) > 0) {
                throw new ClaimLimitException("A clerk cannot settle a claim whose due is above the clerk limit");
            }
        }
        claim.setStatus(settled.name());
        claim.setSettledDue(due);
    }
}
