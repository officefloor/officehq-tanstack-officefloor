package net.officefloor.hq.app.claim;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimRequest {

    public void validate(@RequestBody @Valid ClaimRequest request, Out<ClaimInput> input) throws InvalidClaimException {
        input.set(ClaimForm.check(request));
    }
}
