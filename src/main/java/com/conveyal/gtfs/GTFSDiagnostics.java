// We edited this file

package com.conveyal.gtfs;  // Change this line in GTFSDiagnostics.java

import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class GTFSDiagnostics {

    public static void dumpErrors(Object feed, Logger LOG) {
        try {
            // Locate the 'errors' field on the GTFSFeed instance (gtfs-lib)
            Field fErrors = feed.getClass().getDeclaredField("errors");
            fErrors.setAccessible(true);
            Object store = fErrors.get(feed);
            if (store == null) {
                LOG.warn("gtfs-lib: errors=null");
                return;
            }

            // Normalize the container to a flat Collection<?> of error objects
            Collection<?> all = asCollection(store);
            if (all == null) {
                LOG.warn("gtfs-lib: unknown errors container class={}", store.getClass().getName());
                return;
            }

            // Histogram of error types
            Map<String,Integer> counts = new HashMap<>();
            for (Object e : all) counts.merge(e.getClass().getSimpleName(), 1, Integer::sum);

            int total = all.size();
            List<Map.Entry<String,Integer>> top = counts.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(30)
                .toList();

            LOG.warn("GTFS error types ({} total): {}", total, top);

            // Print up to 10 samples of the most common error type with some useful fields
            if (!top.isEmpty()) {
                String topType = top.get(0).getKey();
                int printed = 0;
                for (Object e : all) {
                    if (e.getClass().getSimpleName().equals(topType)) {
                        LOG.warn("GTFS error sample [{}]: {}", topType, describeError(e));
                        if (++printed == 10) break;
                    }
                }
            }
        } catch (Throwable t) {
            LOG.warn("Could not dump gtfs-lib errors (reflection)", t);
        }
    }

    // --- helpers ---

    private static Collection<?> asCollection(Object store) {
        try {
            if (store instanceof Collection<?> c) return c;

            if (store instanceof Map<?,?> m) {
                List<Object> flat = new ArrayList<>();
                for (Object v : m.values()) {
                    if (v instanceof Collection<?> c2) flat.addAll(c2);
                    else if (v != null) flat.add(v);
                }
                return flat;
            }

            // Try a values() method
            try {
                Method values = store.getClass().getMethod("values");
                Object v = values.invoke(store);
                if (v instanceof Collection<?> c) return c;
            } catch (ReflectiveOperationException ignored) {}

            // Try a toArray()
            try {
                Method toArray = store.getClass().getMethod("toArray");
                Object arr = toArray.invoke(store);
                if (arr instanceof Object[] objs) return Arrays.asList(objs);
            } catch (ReflectiveOperationException ignored) {}

        } catch (Throwable ignored) {}
        return null;
    }

    private static String describeError(Object e) {
        // Best-effort: pull common fields if present; else fall back to toString()
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName());
        for (String fld : new String[]{
            "file", "filename", "table", "entity", "entityId",
            "trip_id", "route_id", "stop_id", "service_id",
            "line", "row", "column", "message", "detail", "badValue"
        }) {
            try {
                Field f = e.getClass().getDeclaredField(fld);
                f.setAccessible(true);
                Object v = f.get(e);
                if (v != null) sb.append(' ').append(fld).append('=').append(v);
            } catch (NoSuchFieldException ignore) {
            } catch (Throwable t) {
                // ignore individual field failures
            }
        }
        try {
            // If the error class has a 'toString' with details, include it
            String ts = e.toString();
            if (ts != null && ts.length() > 0) sb.append(" :: ").append(ts);
        } catch (Throwable ignored) {}
        return sb.toString();
    }
}
