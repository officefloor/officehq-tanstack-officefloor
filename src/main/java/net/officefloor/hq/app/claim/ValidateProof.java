package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ProofRequest> body) {
        body.set(request);
    }
}
