package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;

public class SaveCrew {
    public void save(@Val Crew crew, CrewRepository crews) {
        crews.add(crew);
    }
}
