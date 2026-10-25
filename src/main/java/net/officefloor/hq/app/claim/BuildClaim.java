package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val Person person, @Val ClaimInput input, ClaimRepository repository,
            SheetRepository sheets, Out<Claim> claim) {
        claim.set(repository.insert(person.getSheetId(), person.getId(), input.amount(),
                input.date() != null ? input.date() : sheets.today(person.getSheetId()), input.note(), input.corrects()));
    }
}
