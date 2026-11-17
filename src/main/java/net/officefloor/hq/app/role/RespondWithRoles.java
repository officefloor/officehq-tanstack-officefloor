package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The roles given on the sheet, by user name; read as of a date (see AsOf), those in force at the end of it. */
public class RespondWithRoles {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, RoleRepository roles,
            ObjectResponse<List<Role>> response) {
        String id = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? roles.list(id) : roles.listAsOf(id, date));
    }
}
