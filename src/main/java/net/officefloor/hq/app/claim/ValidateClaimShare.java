package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimShare {
    public void validate(@RequestBody ClaimShareRequest request, Out<ClaimShareChange> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who shares the claim must be named");
        if (request.personId() == null) throw new InvalidSheetRequestException("Choose the person to share the claim with");
        String share = request.share() == null ? "" : request.share().trim();
        if (!share.matches("\\d+(\\.\\d{1,2})?") || new BigDecimal(share).signum() <= 0) {
            throw new InvalidSheetRequestException("A share must be more than 0.00, with up to two decimals");
        }
        actor.set(new Actor(user));
        body.set(new ClaimShareChange(user, new ClaimShare(request.personId(), new BigDecimal(share).setScale(2))));
    }
}
