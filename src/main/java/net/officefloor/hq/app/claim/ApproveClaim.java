package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Approves a PENDING claim: it becomes SETTLED, its due fixed at the figure it shows at that moment
 *  (its live figure, a statement does not bind a PENDING claim, with its late charge on the sheet's
 *  today). Refused unless the user is a steward, and for the user who opened it or last edited it, and for one
 *  with no proofs whose due is above the proof threshold in force on its date. */
public class ApproveClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets)
            throws ClaimStatusException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        String user = by == null ? "" : by;
        if (!ClaimStatus.PENDING.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be approved");
        }
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new ClaimStatusException("Only a steward can approve a claim");
        }
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(user)) {
            throw new ClaimStatusException("The user who opened a claim cannot approve it");
        }
        if (claim.getEditedBy() != null && claim.getEditedBy().equals(user)) {
            throw new ClaimStatusException("The user who last edited a claim cannot approve it");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()));
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        String due = Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                late.rounding(claim.getRoundDate())), settings.floor(claim.getSheetId()).on(claim.getDate()), late);
        SettleClaim.requireProof(claim, person, due, settings);
        claim.setStatus(ClaimStatus.PENDING.moveTo(ClaimStatus.SETTLED).name());
        claim.setSettledDue(due);
    }
}
