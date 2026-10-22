package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and an alias that is not blank. */
public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before giving people aliases.");
        }
        String alias = request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidPersonException("An alias is required.");
        }
        if (alias.length() > 200) {
            throw new InvalidPersonException("An alias must be at most 200 characters.");
        }
        body.set(new AliasInput(request.user().trim(), alias));
    }
}
