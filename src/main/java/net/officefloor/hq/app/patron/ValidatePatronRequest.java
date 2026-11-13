package net.officefloor.hq.app.patron;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the patron the row names; a body with none is refused (400). */
public class ValidatePatronRequest {
    public void validate(@RequestBody @Valid PatronRequest request, Out<PatronRequest> body) {
        body.set(request);
    }
}
