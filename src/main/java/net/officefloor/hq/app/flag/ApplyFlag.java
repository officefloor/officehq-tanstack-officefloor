package net.officefloor.hq.app.flag;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyFlag {
    // Flagging replaces any flag the person had, and is recorded as PERSON_FLAGGED.
    public void apply(@Val Flag flag, @Val Person person, Flags flags, Sheets sheets, ServerHttpConnection connection,
            Audit audit, Out<FlagResponse> responseOut) {
        flags.set(person.id(), flag.reason(), flag.until());
        audit.record(Audit.line("PERSON_FLAGGED", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                "reason=" + flag.reason(), "until=" + flag.until()));
        responseOut.set(FlagResponse.from(new Flag(person.id(), flag.reason(), flag.until()),
                sheets.today(person.sheetId())));
    }
}
