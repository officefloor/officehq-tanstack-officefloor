package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single whole-app setting: a string key and its string value. Not tied to any domain entity —
 * the key is the primary key, so there is no generated id. Mapped to the {@code app_setting} table
 * created by {@code V23__app_setting.sql}. The first use is the dashboard's {@code asOf} reference
 * date (the fixed day overdue is measured against).
 */
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "setting_key")
    private String key;

    @Column(name = "setting_value")
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
