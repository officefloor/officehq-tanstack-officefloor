package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward undoes the transfer that brought the person to this sheet, on the sheet-today they
 *  arrived: they are transferred back to the sheet they came from by the transfer's own rules (see
 *  {@link TransferPerson}). Refused: anyone but a steward, a person who did not arrive by transfer,
 *  and any later day. */
public class UndoTransfer {
    public void undo(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val TransferUndoRequest request, People people, Transfers transfers, Settings settings, Sheets sheets,
            Rollup rollup, Out<Person> personOut, Out<Transfer> transferOut)
            throws PersonNotFoundException, InvalidSheetRequestException {
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can undo a transfer");
        }
        Person person = people.find(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        java.time.LocalDate arrived = transfers.arrivedOn(personId);
        if (person.getOriginSheet() == null || arrived == null) {
            throw new InvalidSheetRequestException("This person did not arrive by a transfer");
        }
        if (!arrived.equals(sheets.today(sheetId))) {
            throw new InvalidSheetRequestException("A transfer can be undone only on the day it arrived");
        }
        new TransferPerson().transfer(sheetId, personId, new TransferRequest(person.getOriginSheet(), request.user()),
                people, transfers, settings, sheets, rollup, personOut, transferOut);
    }
}
