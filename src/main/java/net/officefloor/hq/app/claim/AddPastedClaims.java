package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AddPastedClaims {
    // Each checked line becomes a DRAFT claim, in order, added as the claim form adds one and recorded as CLAIM_ADDED.
    public void add(@Val PastedLines pasted, Claims claims, Sheets sheets, Settings settings, Holidays holidays, Rollup rollup,
            ServerHttpConnection connection, Audit audit, Out<PasteResponse> result) {
        String user = Audit.user(connection);
        for (PastedLine line : pasted.lines()) {
            Person person = line.person();
            ClaimInput input = line.input();
            LocalDate today = sheets.today(person.sheetId());
            LocalDate date = input.date() != null ? input.date() : settings.entryDate(person.sheetId());
            LocalDate dueBy = holidays.dueBy(person.sheetId(), date, person.termsOn(date, settings));
            Claim claim = claims.add(person.sheetId(), person.id(), input.amount(), date, input.note(), null, dueBy);
            claims.log(claim, user.trim(), today, rollup.figure(claim));
            ClaimResponse shown = ClaimResponse.from(claim);
            audit.record(Audit.line("CLAIM_ADDED", claim.sheetId(), user, "person=" + person.name(),
                    "amount=" + shown.amount(), "date=" + shown.date()));
            if (claim.lateEntry()) {
                audit.record(Audit.line("LATE_ENTRY", claim.sheetId(), user, "person=" + person.name(),
                        "amount=" + shown.amount(), "date=" + shown.date()));
            }
        }
        result.set(new PasteResponse(pasted.lines().size()));
    }
}
