package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The status asked for: OPEN, SETTLED, VOID, DELETED (a delete) or DRAFT (a restore). Publishes it with the user making the change and whether it is a review. */
public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatus> status,
            Out<ChangedBy> changedBy, Out<ClaimReview> review, Out<VoidChoice> voidChoice) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String asked = request == null || request.status() == null ? "" : request.status().trim();
        ClaimStatus parsed;
        try {
            parsed = ClaimStatus.valueOf(asked);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("A status is OPEN, SETTLED or VOID");
        }
        if (parsed == ClaimStatus.PENDING) {
            throw new BadRequestException("A status is OPEN, SETTLED or VOID");
        }
        status.set(parsed);
        changedBy.set(by);
        boolean reviewing = request != null && Boolean.TRUE.equals(request.review());
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reviewing && (parsed == ClaimStatus.VOID || parsed == ClaimStatus.DELETED || parsed == ClaimStatus.DRAFT)) {
            throw new BadRequestException("A review approves (SETTLED) or rejects (OPEN) a claim");
        }
        if (reviewing && parsed == ClaimStatus.OPEN && reason.isEmpty()) {
            throw new BadRequestException("A rejection needs a reason");
        }
        String voidReason = request == null || request.voidReason() == null ? "" : request.voidReason().trim();
        voidChoice.set(new VoidChoice(parsed == ClaimStatus.VOID && !voidReason.isEmpty() ? voidReason : null));
        review.set(reviewing ? new ClaimReview(true, parsed == ClaimStatus.OPEN ? reason : null) : ClaimReview.NONE);
    }
}
