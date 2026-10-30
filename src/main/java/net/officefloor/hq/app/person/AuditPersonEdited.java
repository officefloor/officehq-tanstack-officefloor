package net.officefloor.hq.app.person;

import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonEdited {
    public void audit(@Val Person loaded, @Val PersonRequest request, @Val PersonResponse edited, Sheets sheets,
            ServerHttpConnection connection, Audit audit) {
        // terms= only when the save changed the person's own terms recorded, or recorded them from the beginning of time
        // (terms typed other than those in force today, with no from-date), showing those in force on the sheet's today
        // ("" for none); contact= when it changed the contact ("" when it cleared it).
        Integer typed = request.terms() == null ? null : Integer.valueOf(request.terms());
        boolean fromStart = request.termsFrom() == null
                && !Objects.equals(typed, loaded.ownTermsOn(sheets.today(loaded.sheetId())));
        PersonEditedLine.record(loaded, edited, fromStart, connection, audit);
    }
}
