package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;

/** Comment a claim: the text (trimmed) is kept with who wrote it and the sheet's today, and COMMENT_ADDED is
 *  written. An empty comment is refused and writes nothing to the store or the audit. */
final class ClaimComment {
    private ClaimComment() {
    }

    static Claim apply(String id, long cid, String text, String by, ClaimRepository repository, Audit audit) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("a comment cannot be empty");
        }
        repository.comment(id, cid, by, trimmed);
        audit.record("COMMENT_ADDED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + found.date() + Claim.auditNumber(found.number()) + " text=" + trimmed);
        return found;
    }
}
