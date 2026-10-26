package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class AddClaim {
    // The due-by is fixed now: the claim's date plus the terms of its person's grade on that date.
    public void add(@Val ClaimInput input, @Val Person person, Claims claims, Sheets sheets, Settings settings,
            Out<Claim> claimOut) {
        LocalDate date = input.date() != null ? input.date() : sheets.today(person.sheetId());
        LocalDate dueBy = date.plusDays(settings.terms(person.sheetId(), person.gradeOn(date)));
        claimOut.set(claims.add(person.sheetId(), person.id(), input.amount(), date, input.note(), input.corrects(),
                dueBy));
    }
}
