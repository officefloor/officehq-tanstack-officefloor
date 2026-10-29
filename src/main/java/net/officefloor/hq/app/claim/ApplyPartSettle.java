package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.batch.SettleBatches;
import net.officefloor.hq.app.approver.Approvers;
import net.officefloor.hq.app.flag.Flags;
import net.officefloor.hq.app.holiday.Holidays;
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
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyPartSettle {
    // All or nothing: the amount is split off as the split button does (with its lines and rollup), then the new claim is
    // settled as the settle button does (going PENDING above the clerk limit; with its lines, watchers and rollup), in one
    // transaction. Four eyes and the proof rule are judged on the original claim as loaded (the new claim's opener is the
    // user). If either step is refused, the transaction is rolled back, its lines dropped and the refusal sent.
    // Run outside the pipeline's governance: this step owns its transaction.
    public void apply(@RequestBody(required = false) SplitRequest body, @Val Claim claim, Claims claims, People people,
            Rates rates, Settings settings, Holidays holidays, Sheets sheets, Periods periods, Rollup rollup,
            VoidReasons voidReasons, SettleBatches batches, Flags flags, Approvers approvers, Watches watches, Audit audit,
            PlatformTransactionManager transactions, ServerHttpConnection connection, Out<ClaimResponse> responseOut)
            throws ClaimPartSettleRefusedException {
        String sheet = claim.sheetId();
        String user = Audit.user(connection).trim();
        List<Object[]> lines = new ArrayList<>();
        // Holds the lines until the transaction commits; it writes nothing itself.
        Audit buffer = new Audit(".", null) {
            @Override
            public void record(String entry, LocalDate on) {
                lines.add(new Object[] { entry, on });
            }
        };
        String[] refused = new String[1];
        ClaimResponse settled = new TransactionTemplate(transactions).execute(tx -> {
            try {
                Figures before = rollup.figures(sheet);
                Split[] split = new Split[1];
                new ApplySplit().apply(body, claim, claims, people, settings, holidays, sheets, periods, rollup,
                        connection, r -> { }, s -> split[0] = s);
                new AuditClaimSplit().audit(split[0], people, connection, buffer);
                RollupLines.record(sheet, user, before, rollup, buffer);

                Claim created = split[0].created();
                before = rollup.figures(sheet);
                ClaimResponse[] changed = new ClaimResponse[1];
                ClaimStatusRequest[] request = new ClaimStatusRequest[1];
                new ApplyClaimStatus() {
                    @Override
                    protected Claim judged(Claim c) {
                        return claim;
                    }
                }.apply(ClaimStatus.SETTLED.name(), null, created, claims, people, rates, settings, sheets, periods,
                        rollup, voidReasons, batches, flags, approvers, connection, r -> changed[0] = r, r -> request[0] = r);
                new AuditClaimStatus().audit(created, changed[0], request[0], people, connection, buffer);
                new NotifyWatchers().notifyWatchers(created, changed[0], people, watches, connection, buffer);
                RollupLines.record(sheet, user, before, rollup, buffer);
                return changed[0];
            } catch (Exception e) {
                refused[0] = e.getMessage() == null ? "The claim could not be part-settled." : e.getMessage();
                tx.setRollbackOnly();
                return null;
            }
        });
        if (settled == null) {
            throw new ClaimPartSettleRefusedException(refused[0]);
        }
        for (Object[] line : lines) {
            audit.record((String) line[0], (LocalDate) line[1]);
        }
        responseOut.set(settled);
    }
}
