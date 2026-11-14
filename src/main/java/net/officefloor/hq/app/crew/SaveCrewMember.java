package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;

public class SaveCrewMember {
    public void save(@Val NewCrewMember added, CrewRepository crews) {
        crews.addMember(added.crew(), added.member());
    }
}
