/*
 * Copyright 2010-2017 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.oxalis.statistics.guice;

import com.google.inject.*;
import com.google.inject.name.Names;
import no.difi.oxalis.statistics.jdbc.RawStatisticsRepositoryHSqlImpl;
import no.difi.oxalis.statistics.jdbc.RawStatisticsRepositoryMsSqlImpl;
import no.difi.oxalis.statistics.jdbc.RawStatisticsRepositoryMySqlImpl;
import no.difi.oxalis.statistics.jdbc.RawStatisticsRepositoryOracleImpl;
import no.difi.oxalis.persistence.guice.AopJdbcTxManagerModule;
import no.difi.oxalis.statistics.api.RawStatisticsRepository;

/**
 * Wires up the persistence component.
 * <p>
 * NOTE! When creating an injector, remember to supply an instance of {@link javax.sql.DataSource}
 *
 * @author steinar
 *         Date: 25.10.2016
 *         Time: 21.43
 */
public class RawStatisticsRepositoryModule extends AbstractModule {

    public static final String H2 = "H2";

    public static final String MYSQL = "MySQL";

    public static final String MSSQL = "MsSql";

    public static final String ORACLE = "Oracle";

    public static final String HSQLDB = "HSqlDB";

    @Override
    protected void configure() {
        // Includes the Aop based Tx manager, which needs a DataSource
        binder().install(new AopJdbcTxManagerModule());

        bind(Key.get(RawStatisticsRepository.class, Names.named(H2)))
                .to(RawStatisticsRepositoryMsSqlImpl.class);

        bind(Key.get(RawStatisticsRepository.class, Names.named(MYSQL)))
                .to(RawStatisticsRepositoryMySqlImpl.class);

        bind(Key.get(RawStatisticsRepository.class, Names.named(MSSQL)))
                .to(RawStatisticsRepositoryMsSqlImpl.class);

        bind(Key.get(RawStatisticsRepository.class, Names.named(ORACLE)))
                .to(RawStatisticsRepositoryOracleImpl.class);

        bind(Key.get(RawStatisticsRepository.class, Names.named(HSQLDB)))
                .to(RawStatisticsRepositoryHSqlImpl.class);

        bind(RawStatisticsRepository.class)
                .toProvider(RawStatisticsRepositoryProvider.class)
                .in(Singleton.class);
    }
}