package net.officefloor.hq.app.asof;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Hands out connections pointed at the as-of schema while a request is shown as of a date, so
 *  every read in the app sees the sheet as it stood then. The pool resets the schema on return. */
public class AsOfDataSource extends DelegatingDataSource {

    public AsOfDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return point(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return point(super.getConnection(username, password));
    }

    private static Connection point(Connection connection) throws SQLException {
        String schema = AsOfView.schema();
        if (schema != null) {
            connection.setSchema(schema);
        }
        return connection;
    }
}
