package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, ClaimRepository repository,
            Out<Claim> claimOut) {
        claimOut.set(repository.save(new Claim(sheet.getId(), person.getId(), input.amount(),
                input.date() != null ? input.date() : sheet.getToday(), input.note())));
    }
}
