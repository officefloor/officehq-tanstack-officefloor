package net.officefloor.hq.app.alias;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithAliases {
    public void respond(@Val List<Alias> aliases, ObjectResponse<List<AliasResponse>> response) {
        response.send(aliases.stream().map(AliasResponse::from).toList());
    }
}
