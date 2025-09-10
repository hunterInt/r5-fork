# R5 Bundle Export

This R5 fork adds simple functionality to export processed TransportNetwork bundles to files, allowing you to save the processed OSM + GTFS data and avoid rebuilds in your external projects.

## What's Added

- **`BundleExporter`** - A utility class with static methods for exporting bundles
- **`TransportNetwork.exportToFile()`** - Convenience method to export a network
- **`TransportNetwork.loadFromFile()`** - Convenience method to load an exported bundle

## Quick Start

### 1. Export from OSM and GTFS files

```java
import com.conveyal.r5.export.BundleExporter;
import java.util.Arrays;

// Export directly from files
BundleExporter.exportFromFiles(
    "map.osm.pbf", 
    Arrays.asList("gtfs1.zip", "gtfs2.zip"), 
    "my_bundle.dat"
);
```

### 2. Export from a directory

```java
// Directory containing OSM PBF and GTFS ZIP files
BundleExporter.exportFromDirectory("data/", "my_bundle.dat");
```

### 3. Build, modify, then export

```java
import com.conveyal.r5.transit.TransportNetwork;

// Build network from files
TransportNetwork network = TransportNetwork.fromFiles(
    "map.osm.pbf", 
    Arrays.asList("gtfs1.zip", "gtfs2.zip"), 
    null
);

// Export using BundleExporter
BundleExporter.exportNetwork(network, "my_bundle.dat");

// Or use convenience method
network.exportToFile("my_bundle.dat");
```

### 4. Load exported bundle

```java
// Load using BundleExporter
TransportNetwork network = BundleExporter.loadFromFile("my_bundle.dat");

// Or use convenience method
TransportNetwork network = TransportNetwork.loadFromFile("my_bundle.dat");
```

### 5. Stream to OutputStream

```java
import java.io.FileOutputStream;

TransportNetwork network = ...;
try (FileOutputStream fos = new FileOutputStream("output.dat")) {
    BundleExporter.exportNetwork(network, fos);
}
```

## Usage in External Projects

Add this R5 fork as a dependency in your external project, then:

```java
// In your external project
import com.conveyal.r5.export.BundleExporter;
import com.conveyal.r5.transit.TransportNetwork;

public class MyApp {
    public void processTransportData() {
        // Load pre-built bundle - no rebuild needed!
        TransportNetwork network = BundleExporter.loadFromFile("my_bundle.dat");
        
        // Use the network for routing, analysis, etc.
        // ... your code here ...
    }
}
```

## File Format

The exported files use R5's existing Kryo serialization format (same as the internal cache). Files have:
- Header: "R5NETWORK"
- Format version (currently "nv3")  
- R5 commit hash
- Serialized TransportNetwork data

## Benefits

- **No rebuilds**: Skip expensive OSM/GTFS processing in your external projects
- **Fast startup**: Load pre-processed networks in seconds instead of minutes
- **Consistent**: Same data format used internally by R5's caching system
- **Portable**: Bundle files can be shared between applications and environments

## Example

See `examples/BundleExportExample.java` for a complete working example.
