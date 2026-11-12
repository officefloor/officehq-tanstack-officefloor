package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim (a late entry when the period its date falls in is soft-closed), due by its date plus its person's own terms (else the terms of their grade in force on
 *  that date, else the sheet's terms in force then), fixed now, and logs it as DRAFT by the user at its due then. */
public class SaveClaimAdded {
    public void save(@Val ClaimInput input, @Val Person person, ClaimStore store, SettingStore settings,
            ClaimLogStore log, PeriodStore periods, Out<Claim> claim) {
        claim.set(add(input, person, store, settings, log, periods));
    }

    static Claim add(ClaimInput input, Person person, ClaimStore store, SettingStore settings, ClaimLogStore log,
            PeriodStore periods) {
        int terms = termsOf(person, input.date(), settings);
        Claim added = store.add(person.getSheetId(), person.getId(), input.amount(), input.date(), input.note(),
                input.corrects(), input.date().plusDays(terms));
        if (periods.softOn(person.getSheetId(), input.date())) {
            store.markLateEntry(added);
        }
        log.record(added, input.user());
        return added;
    }

    /** The terms a claim of the person dated {@code date} is due by: their own, else their grade's in force
     *  on the date, else the sheet's then. */
    static int termsOf(Person person, java.time.LocalDate date, SettingStore settings) {
        return person.getTerms() != null ? person.getTerms()
                : settings.terms(person.getSheetId(), person.gradeOn(date), date);
    }
}
