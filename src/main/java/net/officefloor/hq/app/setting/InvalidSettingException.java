package net.officefloor.hq.app.setting;

/** The setting's key is not one the sheet has, or its value is not one the key allows. */
public class InvalidSettingException extends Exception {
    public InvalidSettingException(String message) {
        super(message);
    }
}
