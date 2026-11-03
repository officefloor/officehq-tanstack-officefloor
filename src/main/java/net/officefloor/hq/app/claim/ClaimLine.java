package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Add a line to a DRAFT claim: a label (trimmed, required), a quantity (a whole number, 1 or more) and a unit amount
 *  (up to two decimals, 0.00 or more). The claim's amount becomes its lines' quantities times their unit amounts,
 *  added up, and CLAIM_LINE_ADDED is written. Adding a line counts as editing the claim. A claim that is not a DRAFT,
 *  in a closed period, or locked by another user is refused; every check is made before anything is written, so a
 *  refusal writes nothing to the store or the audit. */
final class ClaimLine {
    private ClaimLine() {
    }

    private static final Pattern QTY = Pattern.compile("\\d+");

    static Claim apply(String id, long cid, String label, String qty, String unit, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("DRAFT")) {
            throw new BadRequestException("only a DRAFT claim can be given a line");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        String holder = repository.lockedBy(id, cid);
        if (holder != null && !holder.equals(by)) {
            throw new BadRequestException("the claim is being edited by " + holder);
        }
        String name = label == null ? "" : label.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("a line needs a label");
        }
        String count = qty == null ? "" : qty.trim();
        if (!QTY.matcher(count).matches() || count.length() > 9 || Integer.parseInt(count) < 1) {
            throw new BadRequestException("a line's quantity must be a whole number of 1 or more");
        }
        int q = Integer.parseInt(count);
        BigDecimal u = ClaimInput.amount(unit == null ? "" : unit.trim());
        BigDecimal amount = repository.addLine(id, cid, name, q, u);
        if (amount == null) {
            throw new BadRequestException("only a DRAFT claim can be given a line");
        }
        repository.editedBy(id, cid, by);
        audit.record("CLAIM_LINE_ADDED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + ClaimRepository.money(amount) + " date=" + found.date() + Claim.auditNumber(found.number())
                + " label=" + name + " qty=" + q + " unit=" + ClaimRepository.money(u));
        return repository.find(id, cid);
    }
}
