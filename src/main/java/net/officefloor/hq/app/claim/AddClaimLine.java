package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a line to a claim, which counts as editing it: refused, like an edit, for a claim that is not a
 *  DRAFT, one due by a day in a closed period and one another user's open editor locks; and for an empty
 *  label, a quantity that is not a whole number of at least 1, and a unit amount with more than two
 *  decimals. The claim's amount becomes its lines' quantities times unit amounts, added up. */
public class AddClaimLine {
    private static final Pattern QTY = Pattern.compile("\\d{1,9}");
    private static final Pattern UNIT = Pattern.compile("\\d{1,15}(\\.\\d{0,2})?|\\.\\d{1,2}");

    public void add(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "label", required = false) String label,
            @RequestParam(name = "qty", required = false) String qty,
            @RequestParam(name = "unit", required = false) String unit, @Val Claim claim,
            PeriodRepository periods, ClaimLineRepository lines, Out<ClaimLine> line)
            throws ClaimStatusException, PeriodClosedException, ClaimLockedException, InvalidClaimException {
        String user = by == null ? "" : by;
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be edited");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (claim.getLockedBy() != null && !claim.getLockedBy().equals(user)) {
            throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy());
        }
        String l = label == null ? "" : label.trim();
        String q = qty == null ? "" : qty.trim();
        String u = unit == null ? "" : unit.trim();
        if (l.isEmpty()) {
            throw new InvalidClaimException("A line needs a label");
        }
        if (!QTY.matcher(q).matches() || Integer.parseInt(q) < 1) {
            throw new InvalidClaimException("A line needs a whole quantity of at least 1");
        }
        if (!UNIT.matcher(u).matches()) {
            throw new InvalidClaimException("A line needs a unit amount with up to two decimals");
        }
        ClaimLine added = new ClaimLine(l, Integer.parseInt(q), new BigDecimal(u).setScale(2));
        BigDecimal before = lines.total(claim);
        claim.setAmount((before == null ? BigDecimal.ZERO : before).add(added.total()).setScale(2).toPlainString());
        claim.setEditedBy(user);
        line.set(added);
    }
}
