package net.officefloor.hq.app.audit;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** A claim's timeline: the audit lines about it, newest first, exactly as the file holds them (see
 *  {@link Audit#timeline}). It reads the file; it records nothing. */
public class LoadClaimTimeline {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            ServerHttpConnection connection, Audit audit, ClaimRepository claims, Out<AuditLines> lines)
            throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Claim claim = claims.find(id, claimId);
        if (claim == null) {
            throw new BadRequestException("no such claim");
        }
        String person = claims.personName(id, claim.personId());
        lines.set(new AuditLines(audit.timeline(id, claim.number(), person, claim.amount(), claim.date())));
    }
}
