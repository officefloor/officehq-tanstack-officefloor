package net.officefloor.hq.app.quickadd;

/** Whether the quick-add creates its person (the name was not on the sheet) or uses one already there. */
public record QuickAddPerson(boolean created) {
}
