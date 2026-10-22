package net.officefloor.hq.app.setting;

/** A settings save with a clerk limit that is not an amount with two decimals, or no signed-in user. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
