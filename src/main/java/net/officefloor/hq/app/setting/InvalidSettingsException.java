package net.officefloor.hq.app.setting;

/** A settings form that cannot be saved: a clerk limit or floor that is not blank nor an amount with up to two decimals. */
public class InvalidSettingsException extends Exception {
    public InvalidSettingsException(String message) {
        super(message);
    }
}
