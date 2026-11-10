package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasInput> body)
            throws InvalidPersonException {
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) throw new InvalidPersonException("Enter an alias");
        if (alias.length() > 200) throw new InvalidPersonException("The alias is too long");
        body.set(new AliasInput(alias));
    }
}
