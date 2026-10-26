package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The user adding a proof to the claim, and its kind: receipt or photo, a receipt when none is chosen. */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ChangedBy> changedBy, Out<ChosenProofKind> kind)
            throws BadRequestException {
        String text = request == null || request.kind() == null ? "" : request.kind().trim();
        if (!text.isEmpty() && !ProofKind.ALL.contains(text)) {
            throw new BadRequestException("A proof is a receipt or a photo");
        }
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
        kind.set(new ChosenProofKind(text.isEmpty() ? null : text));
    }
}
