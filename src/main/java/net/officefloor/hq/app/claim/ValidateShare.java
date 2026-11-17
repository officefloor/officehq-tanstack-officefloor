package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who shares the claim, with whom and the share. */
public class ValidateShare {
    public void validate(@RequestBody ShareRequest request, Out<ClaimSharing> sharing) {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String share = request == null || request.share() == null ? "" : request.share().trim();
        BigDecimal pct = share.matches("\\d{1,3}(\\.\\d{1,2})?") ? new BigDecimal(share).setScale(2) : null;
        sharing.set(new ClaimSharing(by, request == null ? null : request.personId(), pct));
    }
}
