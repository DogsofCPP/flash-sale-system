package com.example.userservice.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> DATA_SOURCE_HOLDER = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return DATA_SOURCE_HOLDER.get();
    }

    public static void setDataSource(String dataSource) {
        DATA_SOURCE_HOLDER.set(dataSource);
    }

    public static void clearDataSource() {
        DATA_SOURCE_HOLDER.remove();
    }

    public static void useMaster() {
        setDataSource("master");
    }

    public static void useSlave() {
        setDataSource("slave");
    }
}
