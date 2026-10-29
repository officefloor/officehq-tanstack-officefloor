package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;

public class AddCrew {
    public void add(@Val Crew crew, Crews crews) {
        crews.add(crew);
    }
}
