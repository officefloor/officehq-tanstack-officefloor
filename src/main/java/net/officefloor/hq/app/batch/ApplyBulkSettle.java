package net.officefloor.hq.app.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.flag.Flags;
import net.officefloor.hq.app.claim.ApplyClaimStatus;
import net.officefloor.hq.app.claim.AuditClaimStatus;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStatusRequest;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.hq.app.watch.NotifyWatchers;
import net.officefloor.hq.app.watch.Watches;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyBulkSettle {
    // All or nothing: the ticked claims are settled in list order, each by the same steps (and the same lines) as settling
    // it alone, in one transaction. If any is refused, the transaction is rolled back and its lines dropped: none is
    // settled and every one counts as refused. Otherwise the claims form the sheet's next batch, whose BATCH_SETTLED
    // line follows the claims' own lines. Run outside the pipeline's governance: this step owns its transaction.
    public void apply(@PathVariable("sheetId") String sheetId, @RequestBody(required = false) BulkSettleRequest body,
            Claims claims, People people, Rates rates, Settings settings, Sheets sheets, Periods periods, Rollup rollup,
            VoidReasons voidReasons, Watches watches, SettleBatches batches, Flags flags, Audit audit,
            PlatformTransactionManager transactions,
            ServerHttpConnection connection, Out<BulkSettleResponse> responseOut) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection).trim();
        List<Long> ids = body == null || body.claimIds() == null ? List.of() : body.claimIds();
        List<Object[]> lines = new ArrayList<>();
        // Holds the lines until the transaction commits; it writes nothing itself.
        Audit buffer = new Audit(".", null) {
            @Override
            public void record(String entry, LocalDate on) {
                lines.add(new Object[] { entry, on });
            }
        };
        Boolean done = new TransactionTemplate(transactions).execute(tx -> {
            boolean ok = !ids.isEmpty() && !settings.isViewer(sheet, user) && !sheets.isArchived(sheet)
                    && ids.stream().distinct().count() == ids.size();
            List<Long> settled = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (int i = 0; ok && i < ids.size(); i++) {
                Long id = ids.get(i);
                Claim claim = id == null ? null : claims.find(sheet, id);
                if (claim == null || periods.closedOn(sheet, claim.dueBy()) != null) {
                    ok = false;
                    break;
                }
                Figures before = rollup.figures(sheet);
                ClaimResponse[] changed = new ClaimResponse[1];
                ClaimStatusRequest[] request = new ClaimStatusRequest[1];
                try {
                    new ApplyClaimStatus().apply(ClaimStatus.SETTLED.name(), null, claim, claims, people, rates,
                            settings, sheets, periods, rollup, voidReasons, batches, flags, connection, r -> changed[0] = r,
                            r -> request[0] = r);
                } catch (Exception refused) {
                    ok = false;
                    break;
                }
                new AuditClaimStatus().audit(claim, changed[0], request[0], people, connection, buffer);
                new NotifyWatchers().notifyWatchers(claim, changed[0], people, watches, connection, buffer);
                RollupLines.record(sheet, user, before, rollup, buffer);
                settled.add(claim.id());
                if (ClaimStatus.SETTLED.name().equals(changed[0].status()) && changed[0].settledDue() != null) {
                    total = total.add(new BigDecimal(changed[0].settledDue()));
                }
            }
            if (!ok) {
                tx.setRollbackOnly();
                return false;
            }
            int number = batches.add(sheet, user, settled);
            buffer.record(Audit.line("BATCH_SETTLED", sheet, user, "batch=" + number, "count=" + settled.size(),
                    "total=" + total.toPlainString()));
            return true;
        });
        if (Boolean.TRUE.equals(done)) {
            for (Object[] line : lines) {
                audit.record((String) line[0], (LocalDate) line[1]);
            }
            responseOut.set(new BulkSettleResponse(ids.size(), 0, batches.last(sheet)));
        } else {
            responseOut.set(new BulkSettleResponse(0, ids.size(), batches.last(sheet)));
        }
    }
}
