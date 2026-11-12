package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Splits the amount off the OPEN claim, which keeps the rest unbound from any statement figure, into a new
 *  OPEN claim of the same date and note, numbered next (see {@link Numbering}), opened by the user, due by
 *  its date plus the terms of its person's grade in force on that date (else the sheet's terms then), fixed
 *  now, and logged as OPEN by the user. Refused when another user moved the claim on first. */
public class ApplySplit {
    public void apply(@Val SplitInput input, @Val Claim claim, ClaimStore store, PersonStore people, SettingStore settings,
            SheetStore sheets, PeriodStore periods, ClaimLogStore log, Out<Split> split) throws InvalidSheetException {
        if (!store.shrink(claim, claim.getAmount().subtract(input.amount()))) {
            throw new InvalidSheetException("Only an OPEN claim can be split");
        }
        String sheetId = claim.getSheetId();
        Person person = people.find(sheetId, claim.getPersonId());
        int terms = settings.terms(sheetId, person.gradeOn(claim.getDate()), claim.getDate());
        String number = Numbering.next(sheetId, claim.getDate(), store, settings, sheets, periods);
        Claim into = store.addSplit(claim, input.amount(), number, input.user(), claim.getDate().plusDays(terms));
        log.record(into, input.user());
        split.set(new Split(into));
    }
}
