package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim to the person; a claim given no date is dated the sheet's today. */
public class AddClaim {
    public void add(@Val Person person, @Val ClaimValues values, SheetRepository sheets, ClaimRepository claims,
            Out<Claim> claim) {
        claim.set(claims.add(person.sheetId(), person.id(), values.amount(),
                values.date() != null ? values.date() : sheets.today(person.sheetId()), values.note()));
    }
}
