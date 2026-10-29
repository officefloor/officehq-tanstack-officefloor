package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;

public class AddNote {
    public void add(@Val Note note, Notes notes) {
        notes.add(note);
    }
}
