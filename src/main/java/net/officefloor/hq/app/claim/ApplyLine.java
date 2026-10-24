package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Adds the line to the claim, dated the sheet's today, and makes the claim's amount its lines' quantities times unit
 *  amounts, added up. Adding a line edits the claim: it is refused unless the claim is a DRAFT, and when its due-by is in
 *  a closed period; it records who last edited it. */
public class ApplyLine {
    public void apply(@Val LineInput line, @Val Claim claim, @Val Sheet sheet, ClaimRepository repository,
            ClaimLineRepository lines, PeriodRepository periods,
            @RequestHeader(name = "X-User", required = false) String user) throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a draft claim can have lines added.");
        }
        if (periods.isClosedOn(sheet.getId(), claim.getDueBy())) {
            throw new InvalidClaimException("No claim can be edited in a closed period.");
        }
        lines.saveAndFlush(new ClaimLine(claim, line.label(), line.qty(), line.unit(), sheet.getToday()));
        claim.setAmount(lines.totalOf(claim));
        claim.setEditedBy(AuditUser.decode(user));
        repository.saveAndFlush(claim);
    }
}
