package net.officefloor.hq.app.quickadd;

import jakarta.validation.Valid;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidateClaimRequest;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks the bar on the person and claim forms' terms before anything is written: a blank name, or an
 * amount the claim form refuses, is refused. The claim is dated the sheet's entry date, else its today, with an empty note.
 */
public class ValidateQuickAddRequest {
    public void validate(@RequestBody @Valid QuickAddRequest request, Out<QuickAddRequest> body,
            Out<ClaimInput> input) throws InvalidPersonException, InvalidClaimException {
        String name = request.name() == null ? "" : request.name().strip();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        new ValidateClaimRequest().validate(new ClaimRequest(request.amount(), "", "", "", "", ""), input);
        body.set(new QuickAddRequest(name, request.amount()));
    }
}
