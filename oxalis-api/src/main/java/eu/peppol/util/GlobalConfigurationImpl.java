/*
 * Copyright (c) 2010 - 2015 Norwegian Agency for Pupblic Government and eGovernment (Difi)
 *
 * This file is part of Oxalis.
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission
 * - subsequent versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence
 *  is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package eu.peppol.util;

import eu.peppol.security.PkiVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

import static eu.peppol.util.PropertyDef.*;

/**
 * Implementation of global configuration of Oxalis to be used by both stand alone and web components.
 * <p>
 * With this class, the concept of an Oxalis home directory is introduced.
 * <p/>
 * <p>See {@link OxalisHomeDirectory} for a description on how the Oxalis home directory is located.</p>
 * <p>
 * <p>
 * This class holds an inner class (enum) {@link PropertyDef}, which defines all the known runtime configurable
 * properties.
 * <p>
 * User: steinar
 * Date: 08.02.13
 * Time: 12:45
 */
public class GlobalConfigurationImpl implements GlobalConfiguration {


    public static final String OXALIS_GLOBAL_PROPERTIES_FILE_NAME = "oxalis-global.properties";
    /**
     * Can not make this static, but there is no need either, since this class is a singleton
     */
    public final Logger log = LoggerFactory.getLogger(GlobalConfigurationImpl.class);
    protected Properties properties;
    private volatile boolean hasBeenVerfied = false;
    private File oxalisHomeDirectory;


    public GlobalConfigurationImpl() {

        // Figures out the Oxalis home directory
        oxalisHomeDirectory = computeOxalisHomeDirectory();

        // Figures out the full path and name of the Oxalis global properties file
        File oxalisGlobalPropertiesFileName = computeOxalisGlobalPropertiesFileName(oxalisHomeDirectory);

        createPropertiesWithReasonableDefaults();

        loadPropertiesFromFile(oxalisGlobalPropertiesFileName);

        modifyVerifyAndLogProperties();
    }


    public GlobalConfigurationImpl(File oxalisHomeDirectory, InputStream inputStream) {
        this.oxalisHomeDirectory = oxalisHomeDirectory;

        createPropertiesWithReasonableDefaults();

        loadPropertiesFromInputStream(inputStream);

        modifyVerifyAndLogProperties();

    }

    protected void modifyVerifyAndLogProperties() {
        modifyProperties();

        areAllRequiredPropertiesSet();

        logProperties();
    }


    /**
     * Normally the Oxalis Global Properties file resides in the Oxalis home directory
     */
    protected File computeOxalisGlobalPropertiesFileName(File homeDirectory) {
        log.info("Oxalis home directory: " + homeDirectory);
        return new File(homeDirectory, OXALIS_GLOBAL_PROPERTIES_FILE_NAME);
    }

    protected File computeOxalisHomeDirectory() {
        return new OxalisHomeDirectory().locateDirectory();
    }

    protected void modifyProperties() {

        // TransmissionBuilderOverride may be set to true if in TEST mode or the "secret" property
        // has been set.
        if (OperationalMode.TEST.equals(getModeOfOperation()) ||
                "trUe".equalsIgnoreCase(System.getenv("oxalis.transmissionbuilder.override"))) {
            log.warn("Running with transmissionBuilderOverride enabled since ENVIRONMENT variable oxalis.transmissionbuilder.override=TRUE or mode=TEST");
            properties.setProperty(TRANSMISSION_BUILDER_OVERRIDE.getPropertyName(), Boolean.TRUE.toString());
        }
    }

    protected void createPropertiesWithReasonableDefaults() {
        properties = new Properties(PropertyDef.getDefaultPropertyValues());
        properties.setProperty(KEYSTORE_PATH.getPropertyName(), oxalisHomeDirectory + "/oxalis-keystore.jks");
    }

    protected void areAllRequiredPropertiesSet() {
        if (hasBeenVerfied)
            return;

        log.info("Verifying properties ....");
        for (PropertyDef propertyDef : PropertyDef.values()) {
            if (propertyDef.isRequired() && propertyDef.dumpValue(properties) == null) {
                throw new IllegalStateException("Property " + propertyDef.getPropertyName() + " is required, please inspect your config file");
            }
        }
        hasBeenVerfied = true;
    }


    protected void loadPropertiesFromFile(File propFile) throws IllegalStateException {
        log.debug("Loading configuration properties from " + propFile.getAbsolutePath());

        if (!propFile.isFile() || !propFile.canRead()) {
            log.error("Unable to load the Oxalis global configuration from " + propFile.getAbsolutePath());
            throw new IllegalStateException("Unable to locate the Global configuration file: " + propFile.getAbsolutePath());
        }

        InputStreamReader inputStreamReader = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(propFile);

            inputStreamReader = loadPropertiesFromInputStream(fileInputStream);

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to open " + propFile + "; " + e, e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read from " + propFile + "; " + e, e);
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new IllegalStateException("Unable to close file " + propFile.getAbsolutePath());
                }
            }
        }
    }

    protected InputStreamReader loadPropertiesFromInputStream(InputStream inputStream) {
        InputStreamReader inputStreamReader;
        inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            properties.load(inputStreamReader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load properties from inputStream " + e.getMessage(), e);
        }
        return inputStreamReader;
    }

    /**
     * Allows access to the internal properties object for any extended classes
     */
    protected Properties getProperties() {
        return properties;
    }

    void logProperties() {
        for (PropertyDef propertyDef : PropertyDef.values()) {
            if (!propertyDef.isHidden()) {
                log.info(propertyDef.getPropertyName() + " = " + propertyDef.dumpValue(properties));
            }
        }
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String getJdbcDriverClassName() {
        return JDBC_DRIVER_CLASS.getValue(properties);
    }

    @Override
    public String getJdbcConnectionURI() {
        return JDBC_URI.getValue(properties);
    }

    @Override
    public String getJdbcUsername() {
        return JDBC_USER.getValue(properties);
    }

    @Override
    public String getJdbcPassword() {
        return JDBC_PASSWORD.getValue(properties);
    }

    /**
     * @deprecated supporting JNDI data sources is going to be deprecated
     */
    @Override
    public String getDataSourceJndiName() {
        return JNDI_DATA_SOURCE.getValue(properties);
    }

    @Override
    public String getJdbcDriverClassPath() {
        return JDBC_DRIVER_CLASS_PATH.getValue(properties);
    }

    @Override
    public String getJdbcDialect() {
        return JDBC_DIALECT.getValue(properties);
    }

    /**
     * Location of the Difi private key, which belongs to oxalis-statistics-public.key
     *
     * @return path to location of private key.
     */
    @Override
    public String getStatisticsPrivateKeyPath() {
        return STATISTICS_PRIVATE_KEY_PATH.getValue(properties);
    }

    @Override
    public String getKeyStoreFileName() {
        return KEYSTORE_PATH.getValue(properties);
    }

    @Override
    public String getKeyStorePassword() {
        return KEYSTORE_PASSWORD.getValue(properties);
    }

    @Override
    public String getTrustStorePassword() {
        return TRUSTSTORE_PASSWORD.getValue(properties);
    }

    @Override
    public String getInboundMessageStore() {
        return INBOUND_MESSAGE_STORE.getValue(properties);
    }

    @Override
    public String getPersistenceClassPath() {
        return OXALIS_PERSISTENCE_CLASS_PATH.getValue(properties);
    }

    @Override
    public String getInboundLoggingConfiguration() {
        return INBOUND_LOGGING_CONFIG.getValue(properties);
    }

    @Override
    public PkiVersion getPkiVersion() {
        return PkiVersion.valueOf(PKI_VERSION.getValue(properties));
    }

    @Override
    public OperationalMode getModeOfOperation() {
        return OperationalMode.valueOf(OPERATION_MODE.getValue(properties));
    }

    @Override
    public Integer getConnectTimeout() {
        return Integer.parseInt(CONNECTION_TIMEOUT.getValue(properties));
    }

    @Override
    public Integer getReadTimeout() {
        return Integer.parseInt(READ_TIMEOUT.getValue(properties));
    }

    @Override
    public File getOxalisHomeDir() {
        return oxalisHomeDirectory;
    }

    @Override
    public String getSmlHostname() {
        return SML_HOSTNAME.getValue(properties);
    }

    @Override
    public String getValidationQuery() {
        return JDBC_VALIDATION_QUERY.getValue(properties);
    }

    @Override
    public Boolean isTransmissionBuilderOverride() {
        return Boolean.valueOf(TRANSMISSION_BUILDER_OVERRIDE.getValue(properties));
    }

    /**
     * This is here to assist UNIT tests only, and should NOT be used in production.
     * Makes it possible to override in runtime as well as using environment variable
     */
    @Override
    public void setTransmissionBuilderOverride(Boolean transmissionBuilderOverride) {
        properties.setProperty(TRANSMISSION_BUILDER_OVERRIDE.getPropertyName(), transmissionBuilderOverride.toString());
    }
}