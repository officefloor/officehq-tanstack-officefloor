package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A share is more than 0.00 with at most two decimals, and names a person. */
public class ValidateShare {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ShareRequest request, Out<ShareRequest> body, Settings settings)
            throws ViewerRefusedException, InvalidClaimException {
        settings.refuseViewer(sheetId, request.user());
        String share = request.share() == null ? "" : request.share().trim();
        if (!share.matches("\\d{1,3}(\\.\\d{1,2})?") || new java.math.BigDecimal(share).signum() <= 0) {
            throw new InvalidClaimException("A share is more than 0.00 with at most two decimals");
        }
        if (request.personId() == null || !request.personId().trim().matches("\\d+")) {
            throw new InvalidClaimException("Choose a person to share the claim with");
        }
        body.set(new ShareRequest(request.user(), request.personId().trim(), share));
    }
}
