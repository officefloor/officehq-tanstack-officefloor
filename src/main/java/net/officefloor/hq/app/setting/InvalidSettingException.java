package net.officefloor.hq.app.setting;

/** A setting or role the sheet does not accept; the change is refused and nothing is written. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
