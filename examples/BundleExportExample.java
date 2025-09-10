import com.conveyal.r5.export.BundleExporter;
import com.conveyal.r5.transit.TransportNetwork;

import java.io.IOException;
import java.util.Arrays;

/**
 * Example demonstrating how to use R5's bundle export functionality.
 * 
 * This shows how to:
 * 1. Build a TransportNetwork from OSM and GTFS files
 * 2. Export it to a binary file to avoid rebuilds
 * 3. Load the exported bundle in another application
 */
public class BundleExportExample {
    
    public static void main(String[] args) {
        try {
            // Example 1: Export from individual files
            exportFromFiles();
            
            // Example 2: Export from a directory containing OSM and GTFS files
            exportFromDirectory();
            
            // Example 3: Build network, then export
            buildAndExport();
            
            // Example 4: Load previously exported bundle
            loadExportedBundle();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Export directly from OSM and GTFS files
     */
    private static void exportFromFiles() throws IOException {
        System.out.println("=== Exporting from individual files ===");
        
        String osmFile = "data/map.osm.pbf";
        String[] gtfsFiles = {"data/gtfs1.zip", "data/gtfs2.zip"};
        String outputFile = "exported_bundle.dat";
        
        // Export the bundle - this builds the network and saves it
        BundleExporter.exportFromFiles(osmFile, Arrays.asList(gtfsFiles), outputFile);
        
        System.out.println("Bundle exported to: " + outputFile);
    }
    
    /**
     * Export from a directory containing OSM PBF and GTFS ZIP files
     */
    private static void exportFromDirectory() throws IOException {
        System.out.println("\n=== Exporting from directory ===");
        
        String inputDirectory = "data/";
        String outputFile = "directory_bundle.dat";
        
        // Export from directory - automatically finds OSM and GTFS files
        BundleExporter.exportFromDirectory(inputDirectory, outputFile);
        
        System.out.println("Bundle exported to: " + outputFile);
    }
    
    /**
     * Build network first, then export (useful if you need to modify the network)
     */
    private static void buildAndExport() throws IOException {
        System.out.println("\n=== Build then export ===");
        
        String osmFile = "data/map.osm.pbf";
        String[] gtfsFiles = {"data/gtfs1.zip", "data/gtfs2.zip"};
        
        try {
            // Build the network
            TransportNetwork network = TransportNetwork.fromFiles(osmFile, Arrays.asList(gtfsFiles), null);
            
            // You could modify the network here if needed
            // network.someModification();
            
            // Export using the BundleExporter
            BundleExporter.exportNetwork(network, "custom_bundle.dat");
            
            // Or export using the convenience method on TransportNetwork
            network.exportToFile("convenience_bundle.dat");
            
            System.out.println("Network built and exported");
            
        } catch (Exception e) {
            throw new IOException("Failed to build and export network", e);
        }
    }
    
    /**
     * Load a previously exported bundle
     */
    private static void loadExportedBundle() throws IOException {
        System.out.println("\n=== Loading exported bundle ===");
        
        String bundleFile = "exported_bundle.dat";
        
        // Load using BundleExporter
        TransportNetwork network1 = BundleExporter.loadFromFile(bundleFile);
        
        // Or load using TransportNetwork convenience method
        TransportNetwork network2 = TransportNetwork.loadFromFile(bundleFile);
        
        System.out.println("Bundle loaded successfully!");
        System.out.println("Street vertices: " + network1.streetLayer.vertexStore.nVertices());
        System.out.println("Transit stops: " + network1.transitLayer.getStopCount());
    }
}
