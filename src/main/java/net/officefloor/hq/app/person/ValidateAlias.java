package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the alias asked for, trimmed: refused when it is blank. */
public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasRequest> body)
            throws InvalidPersonException {
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidPersonException("An alias is required");
        }
        body.set(new AliasRequest(alias));
    }
}
