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
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Re-settles a SETTLED claim: its due is fixed again at the figure it would have today as an OPEN claim
 *  bound by no statement (its amount at its person's grade's rate in force on its date, at least the sheet's
 *  floor, with its late charge on the sheet's today; with no rate, none). It stays SETTLED and keeps its
 *  binding. Refused unless the user is a steward, for any other status, and for a claim in a closed period. */
public class ResettleClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            Out<Resettlement> resettlement) throws ClaimStatusException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        if (!ClaimStatus.SETTLED.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be re-settled");
        }
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can re-settle a claim");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()));
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        String live = rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                settings.floor(claim.getSheetId()), late.rounding(claim.getRoundDate()));
        // Figured as an open claim with no fixed due and no statement binding it.
        String old = claim.getSettledDue();
        boolean stated = claim.isStated();
        String statedDue = claim.getStatedDue();
        claim.setStatus(ClaimStatus.OPEN.name());
        claim.setSettledDue(null);
        claim.bind(false, null);
        String due = Dues.shown(claim, live, late);
        claim.bind(stated, statedDue);
        claim.setStatus(ClaimStatus.SETTLED.name());
        claim.setSettledDue(due);
        resettlement.set(new Resettlement(old, due));
    }
}
