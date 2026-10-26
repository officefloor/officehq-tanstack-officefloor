package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The alias to give: trimmed, required, at most 200 characters, as a name is. Publishes it, with the user making the change. */
public class ValidateAlias {
    public void validate(@RequestBody AliasRequest request, Out<AliasInput> alias, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String typed = request == null || request.alias() == null ? "" : request.alias().trim();
        if (typed.isEmpty()) {
            throw new BadRequestException("Enter an alias");
        }
        if (typed.length() > 200) {
            throw new BadRequestException("An alias is at most 200 characters");
        }
        alias.set(new AliasInput(typed));
        changedBy.set(by);
    }
}
