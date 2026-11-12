package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim (a late entry when the period its due-by falls in is soft-closed), due by the business day its person's own terms (else the terms of the grade they have on
 *  the day it is added, the sheet's today, else the sheet's terms in force then), each in force on that day, after its date, skipping weekends and the sheet's holidays, fixed now, and logs it as DRAFT by the user at its due then. */
public class SaveClaimAdded {
    public void save(@Val ClaimInput input, @Val Person person, ClaimStore store, SettingStore settings,
            ClaimLogStore log, PeriodStore periods, HolidayStore holidays, Out<Claim> claim) {
        claim.set(add(input, person, store, settings, log, periods, holidays));
    }

    static Claim add(ClaimInput input, Person person, ClaimStore store, SettingStore settings, ClaimLogStore log,
            PeriodStore periods, HolidayStore holidays) {
        int terms = termsOf(person, settings);
        LocalDate dueBy = holidays.businessDaysAfter(person.getSheetId(), input.date(), terms);
        Claim added = store.add(person.getSheetId(), person.getId(), input.amount(), input.date(), input.note(),
                input.corrects(), dueBy, terms, input.ref(), input.tags());
        if (periods.softOn(person.getSheetId(), dueBy)) {
            store.markLateEntry(added);
        }
        log.record(added, input.user());
        return added;
    }

    /** The terms a claim of the person added now is due by, whatever its date: those in force on the day it is added (the
     *  sheet's today): their own then, else the terms of the grade they have then, else the sheet's then. */
    static int termsOf(Person person, SettingStore settings) {
        LocalDate day = settings.termsDay(person.getSheetId());
        Integer own = person.termsOn(day);
        return own != null ? own : settings.terms(person.getSheetId(), person.gradeOn(day), day);
    }
}
