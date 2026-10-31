package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/** The figure a claim shows and totals: the due fixed when it was settled, else the figure its
 *  person's last statement bound it to, else live at its person's grade's rate in force on the
 *  claim's date, at least the sheet's floor (null, n/a, when there is no rate). An overdue claim's
 *  due adds its late charge on top of that figure. */
@Service
public class Figures {

    private final Rates rates;
    private final Settings settings;
    private final Sheets sheets;

    public Figures(Rates rates, Settings settings, Sheets sheets) {
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The claim's due as the screen shows and totals it: its figure, plus its late charge while it
     *  is overdue. A settled claim's fixed due already holds the charge it had then. */
    public BigDecimal due(Claim claim, String grade) {
        BigDecimal figure = figure(claim, grade);
        if (figure == null || claim.getDue() != null) return figure;
        return figure.add(charge(claim, figure));
    }

    /** The claim's figure without any late charge: the fixed due, else the bound figure, else live. */
    public BigDecimal figure(Claim claim, String grade) {
        if (claim.getDue() != null) return claim.getDue();
        if (claim.isStated()) return claim.getStatedDue();
        return live(claim, grade);
    }

    /** The late charge on a figure of the claim: nothing unless the claim is OPEN and its due-by (its
     *  date plus the sheet's terms) is before the sheet's today; then the sheet's late percent of the
     *  figure for every full 30 days it is overdue (a correction has none), rounded half to even to two decimals. Never bound
     *  by a statement: it follows the sheet's today. */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.getStatus() != ClaimStatus.OPEN || claim.isCorrection()) return none;
        String sheetId = claim.getSheetId();
        LocalDate dueBy = claim.getDate().plusDays(settings.terms(sheetId));
        long periods = ChronoUnit.DAYS.between(dueBy, sheets.today(sheetId)) / 30;
        if (periods <= 0) return none;
        return figure.multiply(settings.latePct(sheetId)).multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The claim's due at its person's grade's rate in force on the claim's date: amount times the
     *  rate divided by 100, rounded half to even to two decimals, raised to the sheet's floor when it
     *  is below it (a correction has no floor); null when there is no rate. */
    public BigDecimal live(Claim claim, String grade) {
        BigDecimal pct = rates.pct(claim.getSheetId(), grade, claim.getDate());
        if (pct == null) return null;
        BigDecimal due = claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN);
        return claim.isCorrection() ? due : due.max(settings.floor(claim.getSheetId()));
    }
}
