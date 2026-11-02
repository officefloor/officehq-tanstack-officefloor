package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a line to a DRAFT claim: a label, a quantity (a whole number, at least 1) and a unit amount (up to two
 * decimals). The claim's amount becomes the quantities times the unit amounts added up, and the line counts as an edit
 * of the claim: CLAIM_LINE_ADDED is written naming the claim with its new amount and ending label=<label> qty=<qty>
 * unit=<unit>. A line on a claim that is not a DRAFT, due inside a closed period, or locked by another user's editor is
 * refused.
 */
public class AddClaimLine {
    private static final Pattern QTY = Pattern.compile("\\d{1,9}");
    private static final Pattern UNIT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void line(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody LineRequest request, @Val Claim claim, Claims claims, People people, Periods periods,
            Audit audit, Rollup rollup) throws InvalidClaimException {
        String label = request == null || request.label() == null ? "" : request.label().trim();
        String qtyText = request == null || request.qty() == null ? "" : request.qty().trim();
        String unitText = request == null || request.unit() == null ? "" : request.unit().trim();
        if (label.isEmpty()) {
            throw new InvalidClaimException("A line needs a label");
        }
        if (!QTY.matcher(qtyText).matches() || Integer.parseInt(qtyText) < 1) {
            throw new InvalidClaimException("A line's quantity must be a whole number, at least 1");
        }
        if (!UNIT.matcher(unitText).matches()) {
            throw new InvalidClaimException("A line's unit amount must be a number with up to two decimals");
        }
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be edited; only a DRAFT can");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimException("A claim due inside a closed period may not be edited");
        }
        if (claim.lockedBy() != null && !claim.lockedBy().equals(user)) {
            throw new InvalidClaimException("The claim is being edited by " + claim.lockedBy());
        }
        int qty = Integer.parseInt(qtyText);
        BigDecimal unit = new BigDecimal(unitText).setScale(2);
        BigDecimal total = BigDecimal.valueOf(qty).multiply(unit);
        for (ClaimLine line : claim.lines()) {
            total = total.add(BigDecimal.valueOf(line.qty()).multiply(new BigDecimal(line.unit())));
        }
        total = total.setScale(2);
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the claim still being a draft with the amount read and not locked by another user's editor.
        if (!claims.addLine(sheet, claim.id(), user, new BigDecimal(claim.amount()), total, label, qty, unit)) {
            throw new InvalidClaimException("The claim is no longer a DRAFT, or another user is editing it");
        }
        Person person = people.find(sheet, claim.person());
        audit.record("CLAIM_LINE_ADDED sheet=" + sheet + " by=" + user + " "
                + claim.withAmount(total.toPlainString()).auditFields(person.name()) + " label=" + label + " qty="
                + qty + " unit=" + unit.toPlainString());
        rollup.record(sheet, user, before);
    }
}
