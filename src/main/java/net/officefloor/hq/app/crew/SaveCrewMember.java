package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;

public class SaveCrewMember {
    public void save(@Val CrewMember member, Crews crews) {
        crews.addMember(member.sheetId(), member.crew(), member.personId());
    }
}
