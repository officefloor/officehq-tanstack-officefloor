package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPatrons {
    public void respond(@Val List<Patron> patrons, ObjectResponse<List<PatronResponse>> response) {
        response.send(patrons.stream().map(PatronResponse::from).toList());
    }
}
