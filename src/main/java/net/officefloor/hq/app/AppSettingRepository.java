package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link AppSetting}. A Spring Data bean, injected into the OfficeFloor logic
 * classes that need a whole-app setting — currently {@link DashboardGet} reading the {@code asOf}
 * reference date.
 */
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
