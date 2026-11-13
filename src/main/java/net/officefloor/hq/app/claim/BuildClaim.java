package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A new DRAFT claim; its due-by is fixed now: its date plus the person's own terms, else
 * those of their grade, else the sheet's, each as in force on that date. */
public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            Out<Claim> claim) {
        LocalDate date = input.date() != null ? input.date() : sheets.entryDate(person.sheetId());
        LocalDate dueBy = NewDueBy.of(date, person, settings);
        claim.set(new Claim(null, person.sheetId(), person.id(), input.amount(), date, input.note(), ClaimStatus.DRAFT,
                null, null, null, false, null, input.corrects(), dueBy));
    }
}
