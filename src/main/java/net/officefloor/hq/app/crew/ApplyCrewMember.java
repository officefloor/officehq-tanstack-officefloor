package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person to the crew; refuses a person not on the sheet, the crew's lead, or one already in the crew. */
public class ApplyCrewMember {
    public void apply(@Val CrewMemberRequest request, @Val Crew crew, PersonRepository people,
            CrewMemberRepository members, Out<CrewMember> memberOut, Out<MemberName> nameOut)
            throws InvalidCrewException {
        Person person = people.findByIdAndSheetId(request.personId(), crew.getSheetId())
                .orElseThrow(() -> new InvalidCrewException("There is no such person on the sheet."));
        if (person.getId().equals(crew.getLeadId())) {
            throw new InvalidCrewException("The crew's lead cannot also be a member.");
        }
        if (members.existsByCrewIdAndPersonId(crew.getId(), person.getId())) {
            throw new InvalidCrewException("That person is already in the crew.");
        }
        nameOut.set(new MemberName(person.getName()));
        memberOut.set(members.saveAndFlush(new CrewMember(crew.getId(), person.getId())));
    }

    /** The added member's name. */
    public record MemberName(String name) {
    }
}
