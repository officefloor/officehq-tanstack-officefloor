package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Optional;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * Gives a claim being opened its number. While the sheet's period numbering is on (the period-numbering-from
 * setting is a date and the sheet's today is on or after it) that is the next in its period's own sequence,
 * <period name>-<n>, or X-<n> for a claim dated in no period; otherwise the sheet's next sheet-wide number.
 * {@link #hold} must come first: it holds the sheet's numbers until the change commits or is refused.
 */
final class ClaimNumbering {

    /** The sequence of claims dated in no period. */
    static final String NO_PERIOD = "X";

    private final boolean byPeriod;

    private ClaimNumbering(boolean byPeriod) {
        this.byPeriod = byPeriod;
    }

    /** Decides how the sheet numbers claims opened now, and holds its numbers. */
    static ClaimNumbering hold(String sheetId, ClaimRepository claims, SettingRepository settings, SheetRepository sheets) {
        Optional<LocalDate> from = settings.periodNumberingFrom(sheetId);
        LocalDate today = sheets.findById(sheetId).map((s) -> s.getToday()).orElse(null);
        boolean byPeriod = from.isPresent() && today != null && !today.isBefore(from.get());
        claims.holdClaimNumbers(sheetId);
        return new ClaimNumbering(byPeriod);
    }

    /** Gives the claim its number. */
    void number(Claim claim, ClaimRepository claims, PeriodRepository periods) {
        String sheet = claim.getSheetId();
        if (!byPeriod) {
            claims.takeClaimNumber(sheet);
            claim.setNumber(claims.lastClaimNumber(sheet));
            return;
        }
        String key = periods.findBySheetId(sheet).stream().filter((p) -> p.contains(claim.getDate()))
                .map(Period::getName).findFirst().orElse(NO_PERIOD);
        if (claims.takeSequenceNumber(sheet, key) == 0) {
            claims.startSequence(sheet, key);
        }
        claim.setPeriodNumber(key + "-" + claims.lastSequenceNumber(sheet, key));
    }
}
