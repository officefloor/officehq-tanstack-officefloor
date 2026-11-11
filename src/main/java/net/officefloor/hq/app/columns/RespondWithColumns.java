package net.officefloor.hq.app.columns;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;

/** The people-list columns the signed-in user has switched off. */
public class RespondWithColumns {
    public void respond(ColumnsRepository columns, HttpServletRequest request, ObjectResponse<ColumnsResponse> response) {
        response.send(new ColumnsResponse(columns.hiddenOf(Audit.user(request))));
    }
}
