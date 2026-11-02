package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Part-settles an OPEN claim: splits the amount off it as the Split button does, then settles the new claim as the
 * Settle button does (to PENDING when the clerk limit says so). The four-eyes rules and the proof rule are judged on
 * the claim part-settled, not the new one (whose opener is the user). Any refusal refuses the whole action: its
 * savepoint rolls back and the audit records held meanwhile are dropped.
 */
public class SettlePartClaim {
    public void settle(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val BigDecimal amount,
            @Val Claim claim, ClaimSplitter splitter, ClaimStatusChanger changer, Claims claims, Audit audit,
            PlatformTransactionManager transactions) throws InvalidClaimStatusChangeException {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        audit.hold();
        try {
            transaction.execute((status) -> {
                try {
                    // Judged on the claim part-settled first, so its opener and editor may not settle part of it.
                    changer.vet(sheet, user, claim, ClaimStatusChange.SETTLE, null);
                    Claim part = splitter.split(sheet, user, claim, amount);
                    changer.settlePart(sheet, user, part);
                } catch (InvalidClaimStatusChangeException refused) {
                    throw new Refused(refused);
                }
                return null;
            });
        } catch (Refused refused) {
            audit.discard();
            throw refused.cause;
        } catch (RuntimeException failed) {
            audit.discard();
            throw failed;
        }
        audit.release();
    }

    private static final class Refused extends RuntimeException {
        private final InvalidClaimStatusChangeException cause;

        private Refused(InvalidClaimStatusChangeException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
