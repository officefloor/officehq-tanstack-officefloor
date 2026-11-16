package net.officefloor.hq.app.setting;

/** A setting that cannot be kept: an unknown key, or a value that key does not allow. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
