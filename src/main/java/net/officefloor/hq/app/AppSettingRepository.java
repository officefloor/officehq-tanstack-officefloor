package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link AppSetting}. A Spring Data bean, injected into the OfficeFloor logic
 * classes (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
