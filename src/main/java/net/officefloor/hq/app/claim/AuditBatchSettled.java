package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes a bulk settle once its claims are settled: one BATCH_SETTLED line with its batch number, how many claims are
 * in it (SETTLED or PENDING) and the frozen dues of those SETTLED added up. A bulk settle that took no batch number
 * (it settled nothing) writes nothing.
 */
public class AuditBatchSettled {

    /** The bulk settle's batch number (null: none). */
    public record BatchSettled(Integer batch) {
    }

    public void audit(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            @RequestBody BulkSettleRequest request, SettleBatchRepository batches, ClaimRepository claims, Audit audit,
            ObjectResponse<BatchSettled> response) {
        String token = request == null || request.batch() == null ? "" : request.batch().trim();
        Integer number = token.isEmpty() ? null : batches.numberOf(sheetId, token);
        if (number != null) {
            Map<Long, Integer> all = batches.batches(sheetId);
            List<Claim> inBatch = claims.findAll(sheetId).stream().filter((c) -> number.equals(all.get(c.id())))
                    .toList();
            BigDecimal total = inBatch.stream().filter((c) -> c.status() == ClaimStatus.SETTLED && c.due() != null)
                    .map(Claim::due).reduce(BigDecimal.ZERO, BigDecimal::add);
            audit.record("BATCH_SETTLED sheet=" + sheetId + " by=" + user + " batch=" + number + " count="
                    + inBatch.size() + " total=" + total.setScale(2).toPlainString());
        }
        response.send(new BatchSettled(number));
    }
}
