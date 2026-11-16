package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who adds the proof and its kind, receipt or photo (blank: none chosen); any other kind is refused. */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ClaimProof> proof) throws InvalidClaimStatusException {
        String kind = request == null || request.kind() == null ? "" : request.kind().trim();
        if (!kind.isEmpty() && !SettingRepository.KINDS.contains(kind)) {
            throw new InvalidClaimStatusException("A proof's kind must be receipt or photo");
        }
        proof.set(new ClaimProof(request == null || request.by() == null ? "" : request.by().trim(),
                kind.isEmpty() ? null : kind));
    }
}
