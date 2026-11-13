package net.officefloor.hq.app.asof;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

/**
 * Every connection is handed out reading the store as it stands now, or, for a request reading the sheet as of a date
 * ({@link AsOfView}), reading that date's schema with the date as the sheet's today. Set on every hand-out, so a
 * pooled connection never passes one request's view on to the next.
 */
@Component
public class AsOfConnections implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && !(bean instanceof DelegatingDataSource)) {
            return new DelegatingDataSource(dataSource) {
                @Override
                public Connection getConnection() throws SQLException {
                    return viewed(super.getConnection());
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    return viewed(super.getConnection(username, password));
                }
            };
        }
        return bean;
    }

    private static Connection viewed(Connection connection) throws SQLException {
        AsOfView view = AsOfView.current();
        try (Statement s = connection.createStatement()) {
            if (view == null) {
                s.execute("SET SCHEMA PUBLIC");
                s.execute("SET @asof = NULL");
            } else {
                s.execute("SET SCHEMA " + view.schema());
                s.execute("SET @asof = DATE '" + view.date() + "'");
            }
        }
        return connection;
    }
}
