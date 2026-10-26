package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AddClaim {
    // The due-by is fixed now: the claim's date plus the person's own terms, else the terms of
    // their grade in force on that date (the sheet's when the grade has none).
    // Its status log starts with its DRAFT line, at its due as shown now.
    public void add(@Val ClaimInput input, @Val Person person, Claims claims, Sheets sheets, Settings settings,
            Rollup rollup, ServerHttpConnection connection, Out<Claim> claimOut) {
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(person.sheetId());
        LocalDate dueBy = date.plusDays(person.termsOn(date, settings));
        Claim claim = claims.add(person.sheetId(), person.id(), input.amount(), date, input.note(), input.corrects(),
                dueBy);
        claims.log(claim, Audit.user(connection).trim(), sheets.today(person.sheetId()), rollup.figure(claim));
        claimOut.set(claim);
    }
}
