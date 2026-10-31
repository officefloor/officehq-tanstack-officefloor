package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class SaveShare {
    public void save(@Val ShareRequest request, @Val Claim claim, @Val Person holder, Claims claims) {
        claims.share(claim, holder.getId(), new BigDecimal(request.share()).setScale(2));
        claim.setShares(claims.shares(claim));
    }
}
