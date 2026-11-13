package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * A claim's due as the screen shows it at this moment (null: n/a): its fixed due, else the greater of the sheet's floor
 * in force on its date and its figure — at its person's grade's rate in force on its date, rounded by its policy date, or
 * the figure a statement bound it to — plus the late charge (at the late percent in force on each 30-day boundary) it has
 * on the sheet's today, if overdue.
 */
public final class ClaimDueNow {

    private ClaimDueNow() {
    }

    public static BigDecimal of(Claim claim, SheetRepository sheets, PersonRepository people, RateRepository rates,
            SettingRepository settings, ClaimRepository claims) {
        Person person = people.find(claim.sheetId(), claim.personId());
        BigDecimal rate = person == null ? null
                : Rate.inForce(rates.findAll(claim.sheetId()), person.gradeOn(claim.date()), claim.date());
        LocalDate today = sheets.today(claim.sheetId());
        DatedSettings dated = settings.dated(claim.sheetId());
        return claim.due(rate, dated.floorOn(claim.date()), dated.lateCharge(today),
                settings.rounding(claim.sheetId(), today, claims.findForPerson(claim.sheetId(), claim.personId())));
    }
}
