package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.settings.Settings;
import org.springframework.stereotype.Service;

/** The figure a claim shows and totals: the due fixed when it was settled, else the figure its
 *  person's last statement bound it to, else live at its person's grade's rate in force on the
 *  claim's date, at least the sheet's floor (null, n/a, when there is no rate). */
@Service
public class Figures {

    private final Rates rates;
    private final Settings settings;

    public Figures(Rates rates, Settings settings) {
        this.rates = rates;
        this.settings = settings;
    }

    public BigDecimal figure(Claim claim, String grade) {
        if (claim.getDue() != null) return claim.getDue();
        if (claim.isStated()) return claim.getStatedDue();
        return live(claim, grade);
    }

    /** The claim's due at its person's grade's rate in force on the claim's date: amount times the
     *  rate divided by 100, rounded half to even to two decimals, raised to the sheet's floor when it
     *  is below it; null when there is no rate. */
    public BigDecimal live(Claim claim, String grade) {
        BigDecimal pct = rates.pct(claim.getSheetId(), grade, claim.getDate());
        if (pct == null) return null;
        BigDecimal due = claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN);
        return due.max(settings.floor(claim.getSheetId()));
    }
}
