package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses an add whose name another person on the sheet already has, or a contact another person on the sheet has. */
public class CheckNameFree {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store)
            throws DuplicateNameException, DuplicateContactException {
        if (store.nameTaken(sheetId.trim(), request.name(), null)) {
            throw new DuplicateNameException(request.name());
        }
        if (request.contact() != null && store.contactTaken(sheetId.trim(), request.contact(), null)) {
            throw new DuplicateContactException(request.contact());
        }
    }
}
