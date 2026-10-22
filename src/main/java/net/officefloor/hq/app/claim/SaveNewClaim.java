package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Store the new claim under the person; with no date given it is dated the sheet's today. */
public class SaveNewClaim {
    public void save(@Val ClaimInput input, @Val Person person, SheetRepository sheets, ClaimRepository repository,
            Out<Claim> claim) {
        var date = input.date() != null ? input.date() : sheets.today(person.sheetId());
        claim.set(repository.insert(new Claim(0, person.sheetId(), person.id(), input.amount(), date, input.note())));
    }
}
