package net.officefloor.hq.app.setting;

/** The setting key is unknown or its value is not valid for it, or the role assigned is not clerk or
 *  steward or has no user name: refused and nothing is written. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
