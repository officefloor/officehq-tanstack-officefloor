package net.officefloor.hq.app.setting;

/** A setting or role to save is not valid; nothing is changed. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
