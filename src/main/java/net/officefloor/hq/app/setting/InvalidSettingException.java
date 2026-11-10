package net.officefloor.hq.app.setting;

/** A clerk limit that is neither blank nor a figure of 0.00 or more with up to two decimals. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
