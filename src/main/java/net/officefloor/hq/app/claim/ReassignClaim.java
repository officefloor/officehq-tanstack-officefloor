package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Move a DRAFT, OPEN or PENDING claim to another person on the sheet; only a role the sheet's permission matrix allows
 *  (by default a steward) may. The claim keeps its
 *  number and date, loses any figure a statement bound it to (live again until its new person's next
 *  statement) and is listed as its new person's newest claim. Every check is made before anything is written. */
public class ReassignClaim {
    public void reassign(@PathVariable("sheetId") String sheetId, @RequestBody ReassignRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (request == null || request.claimId() == null) {
            throw new BadRequestException("no such claim");
        }
        if (request.personId() == null) {
            throw new BadRequestException("choose the person to move the claim to");
        }
        if (!settings.allows(id, "reassign", by)) {
            throw new BadRequestException("a " + settings.role(id, by) + " cannot reassign a claim");
        }
        Claim found = repository.find(id, request.claimId());
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT") && !found.open()) {
            throw new BadRequestException("a " + found.status() + " claim cannot be reassigned");
        }
        long to = request.personId();
        if (!repository.personExists(id, to)) {
            throw new BadRequestException("no such person");
        }
        if (to == found.personId()) {
            throw new BadRequestException("the claim is already that person's");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        String fromName = repository.personName(id, found.personId());
        String toName = repository.personName(id, to);
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.reassign(id, found.id(), to)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be reassigned");
        }
        Claim moved = new Claim(found.id(), to, found.amount(), found.date(), found.note(), found.status(),
                found.number(), found.due(), found.openedBy(), false, null, found.corrects(), found.dueBy(), false,
                found.deferredUntil(), found.deferred(), found.proofs(), found.splitFrom(), found.lateEntry());
        audit.record("CLAIM_REASSIGNED sheet=" + id + " by=" + by + " person=" + toName + " amount=" + moved.amount()
                + " date=" + moved.date() + Claim.auditNumber(moved.number()) + " from=" + fromName + " to=" + toName);
        rollup.record(id, by, before);
        claim.set(moved);
    }
}
