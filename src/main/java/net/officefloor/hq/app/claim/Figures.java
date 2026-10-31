package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.rate.Rates;
import org.springframework.stereotype.Service;

/** The figure a claim shows and totals: the due fixed when it was settled, else the figure its
 *  person's last statement bound it to, else live at its person's grade's rate in force on the
 *  claim's date (null, n/a, when there is none). */
@Service
public class Figures {

    private final Rates rates;

    public Figures(Rates rates) {
        this.rates = rates;
    }

    public BigDecimal figure(Claim claim, String grade) {
        if (claim.getDue() != null) return claim.getDue();
        if (claim.isStated()) return claim.getStatedDue();
        return live(claim, grade);
    }

    /** The claim's due at its person's grade's rate in force on the claim's date: amount times the
     *  rate divided by 100, rounded half to even to two decimals; null when there is no rate. */
    public BigDecimal live(Claim claim, String grade) {
        BigDecimal pct = rates.pct(claim.getSheetId(), grade, claim.getDate());
        return pct == null ? null
                : claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
