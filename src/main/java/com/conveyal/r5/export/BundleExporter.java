package com.conveyal.r5.export;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.osmlib.OSM;
import com.conveyal.r5.analyst.cluster.TransportNetworkConfig;
import com.conveyal.r5.kryo.KryoNetworkSerializer;
import com.conveyal.r5.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple utility class for exporting R5 TransportNetwork bundles to files.
 * This allows saving processed OSM + GTFS data to avoid rebuilds in external projects.
 * 
 * Usage example:
 * <pre>
 * // Export from files
 * BundleExporter.exportFromFiles("map.osm.pbf", Arrays.asList("gtfs1.zip", "gtfs2.zip"), "output.dat");
 * 
 * // Export existing network
 * TransportNetwork network = ...;
 * BundleExporter.exportNetwork(network, "output.dat");
 * </pre>
 */
public class BundleExporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(BundleExporter.class);
    
    /**
     * Export a TransportNetwork built from OSM and GTFS files to a binary file.
     * 
     * @param osmFile Path to OSM PBF file
     * @param gtfsFiles List of paths to GTFS ZIP files
     * @param outputFile Path where the exported bundle should be saved
     * @throws IOException if file operations fail
     */
    public static void exportFromFiles(String osmFile, List<String> gtfsFiles, String outputFile) throws IOException {
        exportFromFiles(osmFile, gtfsFiles, null, outputFile);
    }
    
    /**
     * Export a TransportNetwork built from OSM and GTFS files to a binary file with custom config.
     * 
     * @param osmFile Path to OSM PBF file
     * @param gtfsFiles List of paths to GTFS ZIP files
     * @param configFile Path to TransportNetworkConfig JSON file (optional)
     * @param outputFile Path where the exported bundle should be saved
     * @throws IOException if file operations fail
     */
    public static void exportFromFiles(String osmFile, List<String> gtfsFiles, String configFile, String outputFile) throws IOException {
        LOG.info("Building TransportNetwork from OSM file: {} and {} GTFS files", osmFile, gtfsFiles.size());
        
        try {
            // Build the network from input files
            TransportNetwork network = TransportNetwork.fromFiles(osmFile, gtfsFiles, configFile);
            
            // Export to file
            exportNetwork(network, outputFile);
            
        } catch (Exception e) {
            throw new IOException("Failed to build and export TransportNetwork", e);
        }
    }
    
    /**
     * Export a TransportNetwork built from a directory containing OSM and GTFS files.
     * 
     * @param inputDirectory Directory containing OSM PBF and GTFS ZIP files
     * @param outputFile Path where the exported bundle should be saved
     * @throws IOException if file operations fail
     */
    public static void exportFromDirectory(String inputDirectory, String outputFile) throws IOException {
        LOG.info("Building TransportNetwork from directory: {}", inputDirectory);
        
        try {
            // Build the network from directory
            TransportNetwork network = TransportNetwork.fromDirectory(new File(inputDirectory));
            if (network == null) {
                throw new IOException("Failed to build TransportNetwork from directory: " + inputDirectory);
            }
            
            // Export to file
            exportNetwork(network, outputFile);
            
        } catch (Exception e) {
            throw new IOException("Failed to build and export TransportNetwork from directory", e);
        }
    }
    
    /**
     * Export an existing TransportNetwork to a binary file.
     * 
     * @param network The TransportNetwork to export
     * @param outputFile Path where the exported bundle should be saved
     * @throws IOException if file operations fail
     */
    public static void exportNetwork(TransportNetwork network, String outputFile) throws IOException {
        File file = new File(outputFile);
        LOG.info("Exporting TransportNetwork to file: {}", file.getAbsolutePath());
        
        try {
            // Use R5's existing Kryo serialization
            KryoNetworkSerializer.write(network, file);
            LOG.info("Successfully exported TransportNetwork to: {}", file.getAbsolutePath());
            
        } catch (Exception e) {
            throw new IOException("Failed to write TransportNetwork to file: " + outputFile, e);
        }
    }
    
    /**
     * Export an existing TransportNetwork to an OutputStream.
     * 
     * @param network The TransportNetwork to export
     * @param outputStream The OutputStream to write to
     * @throws IOException if write operations fail
     */
    public static void exportNetwork(TransportNetwork network, OutputStream outputStream) throws IOException {
        LOG.info("Exporting TransportNetwork to OutputStream");
        
        try {
            // Create a temporary file and then stream its contents
            File tempFile = File.createTempFile("r5_export_", ".dat");
            try {
                // Write to temp file first
                KryoNetworkSerializer.write(network, tempFile);
                
                // Stream the file contents to the output stream
                Files.copy(tempFile.toPath(), outputStream);
                LOG.info("Successfully exported TransportNetwork to OutputStream");
                
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to write TransportNetwork to OutputStream", e);
        }
    }
    
    /**
     * Load a TransportNetwork from a previously exported bundle file.
     * 
     * @param bundleFile Path to the exported bundle file
     * @return The loaded TransportNetwork
     * @throws IOException if file operations fail
     */
    public static TransportNetwork loadFromFile(String bundleFile) throws IOException {
        File file = new File(bundleFile);
        LOG.info("Loading TransportNetwork from file: {}", file.getAbsolutePath());
        
        try {
            TransportNetwork network = KryoNetworkSerializer.read(file);
            LOG.info("Successfully loaded TransportNetwork from: {}", file.getAbsolutePath());
            return network;
            
        } catch (Exception e) {
            throw new IOException("Failed to read TransportNetwork from file: " + bundleFile, e);
        }
    }
}
