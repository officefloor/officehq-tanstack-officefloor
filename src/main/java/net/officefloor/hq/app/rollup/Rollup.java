package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;

/**
 * The sheet's figures as the screen shows them: each person's claimed and due-total (by person id,
 * in the order they were added), and the sheet's claimed-total and due-total.
 */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal) {

    /** A person's figures: their name, claimed and due-total (n/a when none). */
    public record Figures(String name, String claimed, String dueTotal) {
        boolean sameFigures(Figures other) {
            return other != null && claimed.equals(other.claimed) && dueTotal.equals(other.dueTotal);
        }
    }

    /**
     * The sheet's figures now. A person's claimed is the amounts of their claims that count (OPEN
     * and SETTLED) added up; their due-total the figures of those claims added up, skipping n/a, and
     * n/a when their grade has no rate today and no claim that counts keeps a fixed figure. The
     * sheet's totals add these up, skipping n/a.
     */
    public static Rollup of(String sheetId, LocalDate today, PersonRepository people, ClaimRepository claims,
            RateRepository rates) {
        List<Rate> all = rates.findAll(sheetId);
        List<Claim> sheetClaims = claims.findAll(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.findAll(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean fixed = false;
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId()) continue;
                if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.SETTLED) continue;
                claimed = claimed.add(claim.getAmount());
                if (claim.getSettledDue() != null || claim.isStated()) fixed = true;
                BigDecimal figure = ClaimFigure.of(claim, all, person.getGrade());
                if (figure != null) due = due.add(figure);
            }
            boolean none = Rate.inForce(all, person.getGrade(), today).isEmpty() && !fixed;
            claimedTotal = claimedTotal.add(claimed);
            if (!none) dueTotal = dueTotal.add(due);
            figures.put(person.getId(), new Figures(person.getName(), show(claimed), none ? "n/a" : show(due)));
        }
        return new Rollup(figures, show(claimedTotal), show(dueTotal));
    }

    /**
     * The audit lines for the move from {@code before} to this: one ROLLUP line per person whose
     * figures changed (a person new since counts as changed), then a TOTALS line when either of the
     * sheet's totals changed.
     */
    public List<String> changesSince(Rollup before, String sheetId, String user) {
        List<String> lines = new ArrayList<>();
        String head = " sheet=" + sheetId + " by=" + user;
        people.forEach((id, now) -> {
            if (!now.sameFigures(before.people().get(id))) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + now.dueTotal());
            }
        });
        if (!claimedTotal.equals(before.claimedTotal()) || !dueTotal.equals(before.dueTotal())) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal);
        }
        return lines;
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
