package net.officefloor.hq.app.alias;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every alias on the sheet. */
public class RespondWithAliases {
    public void respond(@PathVariable("sheet") String sheet, Aliases aliases, ObjectResponse<List<Alias>> response) {
        response.send(aliases.list(sheet));
    }
}
