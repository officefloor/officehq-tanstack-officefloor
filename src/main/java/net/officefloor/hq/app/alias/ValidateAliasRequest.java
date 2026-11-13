package net.officefloor.hq.app.alias;

import jakarta.validation.Valid;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the alias the row adds, kept without surrounding spaces; a blank one is refused (400). */
public class ValidateAliasRequest {
    public void validate(@RequestBody @Valid AliasRequest request, Out<AliasRequest> body)
            throws InvalidPersonException {
        String alias = request.alias() == null ? "" : request.alias().strip();
        if (alias.isEmpty()) {
            throw new InvalidPersonException("An alias is required.");
        }
        body.set(new AliasRequest(alias));
    }
}
