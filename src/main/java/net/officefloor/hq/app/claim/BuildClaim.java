package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val ClaimValues values, @Val Person person, Sheets sheets, Out<Claim> claim) {
        claim.set(new Claim(null, person.getSheetId(), person.getId(), values.amount(),
                values.date() != null ? values.date() : sheets.today(person.getSheetId()), values.note(),
                ClaimStatus.DRAFT, null));
    }
}
