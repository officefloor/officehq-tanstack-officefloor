package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Move a claim along its status: open, settle or void, each only from the statuses it allows. */
public class ChangeClaimStatus {
    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            @PathVariable("action") String action, ServerHttpConnection connection, ClaimRepository repository,
            Audit audit, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long cid;
        try {
            cid = Long.parseLong(claimId == null ? "" : claimId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such claim");
        }
        String to;
        List<String> from;
        String event;
        switch (action == null ? "" : action.trim()) {
            case "open" -> { to = "OPEN"; from = List.of("DRAFT"); event = "CLAIM_OPENED"; }
            case "settle" -> { to = "SETTLED"; from = List.of("OPEN"); event = "CLAIM_SETTLED"; }
            case "void" -> { to = "VOID"; from = List.of("DRAFT", "OPEN"); event = "CLAIM_VOIDED"; }
            default -> throw new BadRequestException("no such action");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        // Four eyes: whoever opened a claim cannot settle it.
        if (to.equals("SETTLED") && found.status().equals("OPEN") && by.equals(found.openedBy())) {
            throw new BadRequestException("the user who opened a claim cannot settle it");
        }
        if (!repository.changeStatus(id, cid, from, to)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be moved to " + to);
        }
        // Opening gives the sheet's next number; voiding clears it, but its line carries the number it had.
        Long number = found.number();
        Long had = number;
        String openedBy = found.openedBy();
        if (to.equals("OPEN")) {
            number = had = repository.number(id, cid);
            repository.openedBy(id, cid, by);
            openedBy = by;
        } else if (to.equals("VOID")) {
            repository.clearNumber(id, cid);
            number = null;
        }
        // Settling fixes the claim's due as it is now: the figure its last statement bound it to, else its
        // live due ('n/a' when its grade has no rate, which stays live).
        String due = null;
        String settledDue = "";
        if (to.equals("SETTLED")) {
            BigDecimal now = repository.figure(id, found);
            repository.settleDue(id, cid, now);
            due = ClaimRepository.dueOf(now);
            settledDue = " due=" + (due == null ? "n/a" : due);
        }
        Claim changed = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(), to,
                number, due, openedBy, found.stated(), found.statedDue());
        audit.record(event + " sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + changed.amount() + " date=" + changed.date() + Claim.auditNumber(had) + settledDue);
        claim.set(changed);
    }
}
