package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who adds the proof. */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ClaimProof> proof) {
        proof.set(new ClaimProof(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
