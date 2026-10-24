package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the alias to add, trimmed; refuses a blank one. */
public class ValidateAlias {

    public record AliasRequest(String alias) {
    }

    public void validate(@RequestBody(required = false) AliasRequest request, Out<AliasInput> body)
            throws InvalidPersonException {
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidPersonException("An alias is required.");
        }
        body.set(new AliasInput(alias));
    }
}
