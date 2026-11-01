package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member for a crew the sheet does not have, a person not on the sheet, the crew's lead, or a
 *  person already in the crew. */
public class ValidateCrewMember {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewMemberRequest request,
            Crews crews, Out<CrewMember> member) throws InvalidSheetRequestException {
        Long lead = request == null || request.crew() == null ? null : crews.leadOf(sheetId, request.crew());
        if (lead == null) {
            throw new InvalidSheetRequestException("No such crew");
        }
        String name = request.personId() == null ? null : crews.personName(sheetId, request.personId());
        if (name == null) {
            throw new InvalidSheetRequestException("No such person");
        }
        if (lead.longValue() == request.personId()) {
            throw new InvalidSheetRequestException(name + " already leads " + request.crew());
        }
        if (crews.hasMember(sheetId, request.crew(), request.personId())) {
            throw new InvalidSheetRequestException(name + " is already in " + request.crew());
        }
        member.set(new CrewMember(sheetId, request.crew(), request.personId(), name, request.user()));
    }
}
