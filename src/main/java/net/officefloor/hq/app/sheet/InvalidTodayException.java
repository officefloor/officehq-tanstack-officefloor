package net.officefloor.hq.app.sheet;

public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a date: " + value);
    }
}
