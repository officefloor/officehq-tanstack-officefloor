package net.officefloor.hq.app.person;

/** The audit line for a saved person: the event, sheet and user, then name and grade. */
final class PersonAudit {
    private PersonAudit() {
    }

    static String line(String event, Person person, String user) {
        return event + " sheet=" + person.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " name=" + person.getName() + " grade=" + person.getGrade();
    }
}
