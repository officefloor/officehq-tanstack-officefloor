package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Keeps the line on the claim and makes the claim's amount its lines' quantities times their unit amounts, added up. */
public class AddClaimLine {
    public void add(@Val Claim claim, @Val ClaimLine typed, ClaimLineRepository lines, Out<Claim> updated) {
        ClaimLine line = new ClaimLine(claim.id(), typed.label(), typed.qty(), typed.unit());
        List<ClaimLine> all = new ArrayList<>(lines.find(claim.sheetId(), claim.id()));
        all.add(line);
        lines.append(claim.sheetId(), line);
        updated.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), ClaimLineRepository.total(all),
                claim.date(), claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(),
                claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(),
                claim.proofs(), claim.splitFrom(), claim.added(), claim.waived(), claim.held()));
    }
}
