package net.officefloor.hq.app.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The audit viewer: the sheet's lines read from the audit file (it is not a second record), newest
 * first. With a name in {@code ?name=} only the lines carrying {@code name=} or {@code person=} that
 * name, compared as names are compared (case and surrounding spaces ignored).
 */
public class RespondWithAuditLines {
    public void respond(@Val Sheet sheet, Audit audit, HttpServletRequest request,
            ObjectResponse<AuditLinesResponse> response) {
        String name = request.getParameter("name");
        List<String> lines = new ArrayList<>(audit.linesFor(sheet.id(), name == null ? "" : name));
        Collections.reverse(lines);
        response.send(new AuditLinesResponse(lines));
    }
}
