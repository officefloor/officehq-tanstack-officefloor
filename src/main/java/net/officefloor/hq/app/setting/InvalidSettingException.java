package net.officefloor.hq.app.setting;

/** A setting or a role was given a value it cannot take. */
public class InvalidSettingException extends Exception {

    public InvalidSettingException(String message) {
        super(message);
    }
}
