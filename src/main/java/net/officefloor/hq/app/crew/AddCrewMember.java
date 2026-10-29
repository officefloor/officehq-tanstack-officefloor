package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;

public class AddCrewMember {
    public void add(@Val CrewMember member, Crews crews) {
        crews.addMember(member);
    }
}
