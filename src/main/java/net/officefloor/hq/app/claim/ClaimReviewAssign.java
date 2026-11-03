package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A steward assigns a REVIEW claim to a user (trimmed) to review; assigning it again replaces the user. A blank user,
 *  a claim in any other status, or anyone but a steward is refused. Every check is made before anything is written, so
 *  a refusal writes nothing to the store or the audit. */
final class ClaimReviewAssign {
    private ClaimReviewAssign() {
    }

    static Claim apply(String id, long cid, String user, String by, ClaimRepository repository, SettingsRepository settings,
            Audit audit) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can assign a claim's review");
        }
        if (!found.status().equals("REVIEW")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be assigned for review");
        }
        String who = user == null ? "" : user.trim();
        if (who.isEmpty()) {
            throw new BadRequestException("give the user to assign the review to");
        }
        if (!repository.assignReview(id, cid, who)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be assigned for review");
        }
        audit.record("REVIEW_ASSIGNED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number()) + " user=" + who);
        return found;
    }
}
