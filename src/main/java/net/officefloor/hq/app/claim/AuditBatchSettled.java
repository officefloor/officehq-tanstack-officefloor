package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/** Once every claim of a bulk settle is settled, records its batch: its number, how many claims it holds and the
 *  frozen figures of those SETTLED added up. A key that settled nothing is no batch, and records nothing. */
public class AuditBatchSettled {
    public void audit(@Val Sheet sheet, @PathVariable("batchKey") String batchKey, ClaimRepository repository,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String key = batchKey == null ? "" : batchKey.trim();
        Integer number = key.isEmpty() ? null : repository.batchNumber(sheet.getId(), key);
        if (number == null) {
            return;
        }
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Claim claim : repository.findBySheetIdOrderByIdDesc(sheet.getId())) {
            if (!number.equals(claim.getSettleBatch())) {
                continue;
            }
            count++;
            if (claim.getStatus() == ClaimStatus.SETTLED && claim.getDue() != null) {
                total = total.add(claim.getDue());
            }
        }
        audit.record("BATCH_SETTLED sheet=" + sheet.getId() + " by=" + AuditUser.decode(user) + " batch=" + number
                + " count=" + count + " total=" + total.setScale(2).toPlainString());
    }
}
