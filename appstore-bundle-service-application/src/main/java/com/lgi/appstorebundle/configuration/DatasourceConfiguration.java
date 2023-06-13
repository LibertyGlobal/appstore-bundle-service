/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2023 Liberty Global Technology Services BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lgi.appstorebundle.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Configuration
public class DatasourceConfiguration {

    @Value("${query.timeout.seconds}")
    @Min(1)
    @Max(Integer.MAX_VALUE)
    private int queryTimeoutInSeconds;

    @Value("${datasource.stack.name}")
    private String stackName;

    @Value("${datasource.charset}")
    private String charSet;

    @Configuration
    @ConfigurationProperties(prefix = "spring.datasource.hikari.write")
    public class WriteHikariConfig extends HikariConfig {

        @Bean("writeDataSource")
        public HikariDataSource writeDataSource() {
            return new HikariDataSource(this);
        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "spring.datasource.hikari.read")
    public class ReadHikariConfig extends HikariConfig {

        @Bean("readDataSource")
        public HikariDataSource readDataSource() {
            return new HikariDataSource(this);
        }
    }

    @Bean("readConnectionProvider")
    public DataSourceConnectionProvider readConnectionProvider(HikariDataSource readDataSource) {
        return new DataSourceConnectionProvider
                (new TransactionAwareDataSourceProxy(readDataSource));
    }

    @Bean("writeConnectionProvider")
    public DataSourceConnectionProvider writeConnectionProvider(HikariDataSource writeDataSource) {
        return new DataSourceConnectionProvider
                (new TransactionAwareDataSourceProxy(writeDataSource));
    }

    @Bean
    public DefaultDSLContext readDslContext(@Qualifier("readDataSource") HikariDataSource readDataSource) {
        return new DefaultDSLContext(readConfiguration(readDataSource));
    }

    @Bean
    public DefaultDSLContext writeDslContext(@Qualifier("writeDataSource") HikariDataSource writeDataSource) {
        return new DefaultDSLContext(writeConfiguration(writeDataSource));
    }

    private DefaultConfiguration readConfiguration(HikariDataSource dataSource) {
        DefaultConfiguration basicConfiguration = basicConfiguration(dataSource);
        basicConfiguration.set(readConnectionProvider(dataSource));
        return basicConfiguration;
    }

    private DefaultConfiguration writeConfiguration(HikariDataSource dataSource) {
        DefaultConfiguration basicConfiguration = basicConfiguration(dataSource);
        basicConfiguration.set(writeConnectionProvider(dataSource));
        return basicConfiguration;
    }

    private DefaultConfiguration basicConfiguration(HikariDataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(dataSource)
                .set(SQLDialect.POSTGRES)
                .set(SettingsTools.defaultSettings().withQueryTimeout(queryTimeoutInSeconds));
        return configuration;
    }
}
