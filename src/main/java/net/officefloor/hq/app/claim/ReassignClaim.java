package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Move a DRAFT, OPEN or PENDING claim to another person on the sheet, by the rules of {@link ClaimReassign};
 *  the claim's own person is refused. */
public class ReassignClaim {
    public void reassign(@PathVariable("sheetId") String sheetId, @RequestBody ReassignRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, PersonRepository people, Audit audit, Rollup rollup, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (request == null) {
            throw new BadRequestException("no such claim");
        }
        claim.set(ClaimReassign.apply(id, request.claimId(), request.personId(), false, by, repository, periods,
                settings, people, audit, rollup));
    }
}
