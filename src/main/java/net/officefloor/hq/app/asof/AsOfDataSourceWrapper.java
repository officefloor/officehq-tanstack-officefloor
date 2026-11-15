package net.officefloor.hq.app.asof;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/** Wraps the app's DataSource in {@link AsOfDataSource}. */
@Component
public class AsOfDataSourceWrapper implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && !(bean instanceof AsOfDataSource)) {
            return new AsOfDataSource(dataSource);
        }
        return bean;
    }
}
