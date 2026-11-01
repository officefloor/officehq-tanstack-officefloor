package net.officefloor.hq.app.alias;

/** The alias is blank, or equals (ignoring case) a name or alias already on the sheet. */
public class InvalidAliasException extends Exception {
    public InvalidAliasException(String message) {
        super(message);
    }
}
