package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateProof {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ProofRequest request, Out<ProofRequest> body, Settings settings)
            throws ViewerRefusedException, ClaimStatusRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String kind = request.kind() == null ? "" : request.kind().trim();
        if (!kind.isEmpty() && !ProofKind.KINDS.contains(kind)) {
            throw new ClaimStatusRefusedException("A proof's kind must be receipt or photo");
        }
        body.set(new ProofRequest(request.user(), kind.isEmpty() ? null : kind));
    }
}
