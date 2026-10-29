package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;

/** The rule a claim's lines set on its amount. */
public final class ClaimLines {

    private ClaimLines() {
    }

    /** The lines added up: each quantity times its unit amount, in two decimals. */
    public static BigDecimal total(List<ClaimLine> lines) {
        return lines.stream().map(ClaimLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
