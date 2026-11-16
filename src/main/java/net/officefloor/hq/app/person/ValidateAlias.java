package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the alias (kept without its outer spaces) and the signed-in user are required. */
public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasValues> values) throws InvalidPersonException {
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidPersonException("An alias is required");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user adding the alias is required");
        }
        values.set(new AliasValues(alias, by));
    }
}
