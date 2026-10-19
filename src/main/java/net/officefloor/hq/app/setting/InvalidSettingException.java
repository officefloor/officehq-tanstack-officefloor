package net.officefloor.hq.app.setting;

/** The setting is not known, or its value is not valid for it. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
