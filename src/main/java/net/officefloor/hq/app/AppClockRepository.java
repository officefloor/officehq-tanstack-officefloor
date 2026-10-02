package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link AppClock} — the dashboard's fixed "as of" reference date (single row). */
public interface AppClockRepository extends JpaRepository<AppClock, Long> {
}
