package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;

public class SaveNote {
    public void save(@Val Note note, Notes notes) {
        notes.add(note);
    }
}
