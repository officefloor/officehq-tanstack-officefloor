package net.officefloor.hq.app.person;

/** A person add or edit to a name another person on the sheet, or on another sheet of its tree, already has. */
public class DuplicatePersonNameException extends Exception {
    public DuplicatePersonNameException(String name) {
        super("The name " + name + " is already in use on this sheet.");
    }

    /** The name is already a name or alias on another sheet of this sheet's tree. */
    public DuplicatePersonNameException(String name, String sheetId) {
        super("The name " + name + " is already in use on sheet " + sheetId + ", in this sheet's tree.");
    }

    /** Refuse the name when another sheet of the sheet's tree already has it as a name or an alias. */
    public static void refuseInTree(String sheetId, String name, PersonRepository repository)
            throws DuplicatePersonNameException {
        String other = repository.sheetInTreeUsing(sheetId, name);
        if (other != null) {
            throw new DuplicatePersonNameException(name, other);
        }
    }
}
