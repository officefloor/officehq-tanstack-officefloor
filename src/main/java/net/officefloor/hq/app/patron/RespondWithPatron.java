package net.officefloor.hq.app.patron;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The patron just named, with its share among the person's patrons now. */
public class RespondWithPatron {
    public void respond(@Val Patron patron, PatronRepository patrons, ObjectResponse<PatronResponse> response) {
        response.send(PatronResponse.from(patrons.findAll(patron.sheetId())).stream()
                .filter(p -> p.personId() == patron.personId() && p.patronId() == patron.patronId()).findFirst()
                .orElseThrow());
    }
}
