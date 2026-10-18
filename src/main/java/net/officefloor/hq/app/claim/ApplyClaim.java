package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the claim's amount, date, note and the number it corrects; a claim given no date takes the sheet's today.
 *  A new claim's due-by is fixed from its date and the terms of its person's grade in force on it; an edit keeps it. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim, SheetRepository sheets, PersonRepository people,
            SettingRepository settings) {
        claim.setAmount(input.amount());
        claim.setDate(input.date() != null ? input.date()
                : sheets.findById(claim.getSheetId()).orElseThrow().getToday());
        if (claim.getDueBy() == null) { // a new claim: its due-by is fixed now
            String grade = people.findById(claim.getPersonId()).orElseThrow().gradeOn(claim.getDate());
            claim.setDueBy(claim.getDate().plusDays(settings.terms(claim.getSheetId(), grade)));
        }
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
    }
}
