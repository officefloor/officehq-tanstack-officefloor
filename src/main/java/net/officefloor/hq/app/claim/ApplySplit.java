package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Splits the amount off the OPEN claim, which keeps the rest unbound from any statement figure, into a new
 *  OPEN claim of the same date and note, numbered next in the period of its own due-by (see {@link Numbering}), opened by the user, due by
 *  the business day its person's own terms (else the grade's they have, else the sheet's, each in force on the sheet's today, when it is split off) after its date, fixed
 *  now (a late entry when the period that due-by falls in is soft-closed), and logged as OPEN by the user. Refused when another user moved the claim on first. */
public class ApplySplit {
    public void apply(@Val SplitInput input, @Val Claim claim, ClaimStore store, PersonStore people, SettingStore settings,
            SheetStore sheets, PeriodStore periods, ClaimLogStore log, HolidayStore holidays,
            Out<Split> split) throws InvalidSheetException {
        if (!store.shrink(claim, claim.getAmount().subtract(input.amount()))) {
            throw new InvalidSheetException("Only an OPEN claim can be split");
        }
        String sheetId = claim.getSheetId();
        Person person = people.find(sheetId, claim.getPersonId());
        int terms = SaveClaimAdded.termsOf(person, settings);
        LocalDate dueBy = holidays.businessDaysAfter(sheetId, claim.getDate(), terms);
        String number = Numbering.next(sheetId, dueBy, store, settings, sheets, periods);
        Claim into = store.addSplit(claim, input.amount(), number, input.user(), dueBy, terms);
        if (periods.softOn(sheetId, dueBy)) {
            store.markLateEntry(into);
        }
        log.record(into, input.user());
        split.set(new Split(into));
    }
}
