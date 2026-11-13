package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * A claim's due as the screen shows it at this moment (null: n/a): its fixed due, else its figure at its person's
 * grade's rate in force on its date, raised to the sheet's floor and rounded by its policy date — or the figure a
 * statement bound it to — plus the late charge it has on the sheet's today, if overdue.
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
        return claim.due(rate, settings.floor(claim.sheetId()), settings.lateCharge(claim.sheetId(), today),
                settings.rounding(claim.sheetId(), today, claims.findForPerson(claim.sheetId(), claim.personId())));
    }
}
