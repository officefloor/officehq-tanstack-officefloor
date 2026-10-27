package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateAlias {
    // The alias is kept without surrounding spaces; a blank one is refused.
    public void validate(@RequestBody AliasRequest body, Out<Alias> alias) throws InvalidPersonException {
        String value = body == null || body.alias() == null ? "" : body.alias().trim();
        if (value.isEmpty()) {
            throw new InvalidPersonException("Type an alias.");
        }
        alias.set(new Alias(value));
    }
}
