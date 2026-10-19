package net.officefloor.hq.app.person;

/** A person on a sheet. Mutable so an Apply step changes it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** The version as loaded from the store; moves on once the person is saved. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
