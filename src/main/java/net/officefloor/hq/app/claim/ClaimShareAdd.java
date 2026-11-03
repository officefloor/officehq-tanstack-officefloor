package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Share a claim with another person of its sheet: they hold a percent (two decimals, more than 0.00) of its due, and
 *  their due-total counts their part (see {@link ClaimShare#part}) while its own person's counts the rest. The claim's
 *  own person, a person already holding a share of it, or shares totalling more than 100.00 are refused, as is a
 *  SETTLED, VOID or DELETED claim. Every check is made before anything is written, so a refusal writes nothing to the
 *  store or the audit. */
final class ClaimShareAdd {
    private ClaimShareAdd() {
    }

    static Claim apply(String id, long cid, String with, String share, String by, ClaimRepository repository,
            SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (List.of("SETTLED", "VOID", "DELETED").contains(found.status())) {
            throw new BadRequestException("a " + found.status() + " claim cannot be shared");
        }
        long pid;
        try {
            pid = Long.parseLong(with == null ? "" : with.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("choose a person to share with");
        }
        if (!repository.personExists(id, pid)) {
            throw new BadRequestException("no such person");
        }
        if (pid == found.personId()) {
            throw new BadRequestException("a claim cannot be shared with its own person");
        }
        String typed = share == null ? "" : share.trim();
        if (!typed.matches("\\d+(\\.\\d{1,2})?")) {
            throw new BadRequestException("share must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(typed).setScale(2);
        if (value.signum() <= 0) {
            throw new BadRequestException("share must be more than 0.00");
        }
        BigDecimal total = value;
        for (ClaimShare s : repository.sharesOf(id, cid)) {
            if (s.personId() == pid) {
                throw new BadRequestException("the person already holds a share of the claim");
            }
            total = total.add(new BigDecimal(s.share()));
        }
        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BadRequestException("the claim's shares cannot total more than 100.00");
        }
        Rollup.Snapshot before = rollup.take(id);
        repository.addShare(id, cid, pid, value);
        audit.record("CLAIM_SHARED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number())
                + " with=" + repository.personName(id, pid) + " share=" + value.toPlainString());
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}
