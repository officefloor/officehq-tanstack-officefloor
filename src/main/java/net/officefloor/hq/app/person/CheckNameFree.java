package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses an add whose name another person on the sheet already has. */
public class CheckNameFree {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store)
            throws DuplicateNameException {
        if (store.nameTaken(sheetId.trim(), request.name(), null)) {
            throw new DuplicateNameException(request.name());
        }
    }
}
