package net.officefloor.hq.app.rollover;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LastRolloverRepository extends JpaRepository<LastRollover, String> {
}
