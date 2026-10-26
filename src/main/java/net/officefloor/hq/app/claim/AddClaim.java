package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class AddClaim {
    public void add(@Val ClaimInput input, @Val Person person, Claims claims, Sheets sheets, Out<Claim> claimOut) {
        claimOut.set(claims.add(person.sheetId(), person.id(), input.amount(),
                input.date() != null ? input.date() : sheets.today(person.sheetId()), input.note()));
    }
}
