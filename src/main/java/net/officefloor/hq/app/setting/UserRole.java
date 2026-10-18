package net.officefloor.hq.app.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The role a user name has on a sheet: clerk or steward. A name with none is a clerk. */
@Entity
@Table(name = "user_role")
public class UserRole {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "user_name", nullable = false)
    private String user;

    @Column(name = "role", nullable = false)
    private String role;

    protected UserRole() {
    }

    public UserRole(String sheetId, String user) {
        this.sheetId = sheetId;
        this.user = user;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getUser() {
        return user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
