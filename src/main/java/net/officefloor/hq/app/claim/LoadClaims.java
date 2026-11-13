package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadClaims {
    public void load(@Val Sheet sheet, ClaimRepository repository, Out<List<Claim>> claims) {
        claims.set(repository.findAll(sheet.id()));
    }
}
