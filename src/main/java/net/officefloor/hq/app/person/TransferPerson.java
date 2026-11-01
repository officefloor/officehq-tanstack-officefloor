package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Transfers the person to another sheet by the rules of {@link PersonTransferrer}. */
public class TransferPerson {
    public void transfer(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val TransferRequest body, @Val Person person, PersonTransferrer transferrer) throws InvalidPersonException {
        transferrer.transfer(sheet, user, person, body.to());
    }
}
