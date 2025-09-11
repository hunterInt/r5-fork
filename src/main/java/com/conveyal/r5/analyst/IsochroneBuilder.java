package com.conveyal.r5.analyst;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minimal isoline builder using marching squares over a WebMercator Grid. */
public final class IsochroneBuilder {
    private static final GeometryFactory GF = new GeometryFactory();

    private IsochroneBuilder() {}

    /**
     * Contour the integer travel-time raster into polygons for each cutoff (seconds).
     * NOTE: This is a lean implementation intended for the “good enough” isochrone use case.
     */
    public static List<Geometry> contour(Grid grid, int[] times, int[] cutoffsSeconds) {
        List<Geometry> out = new ArrayList<>();
        for (int cutoff : cutoffsSeconds) {
            Geometry g = contourSingle(grid, times, cutoff);
            out.add(g != null ? g : GF.createPolygon());
        }
        return out;
    }

    /** Build a single isochrone polygon at the given cutoff (seconds). */
    private static Geometry contourSingle(Grid grid, int[] times, int cutoff) {
        int width = grid.extents.width;
        int height = grid.extents.height;
        
        // Early exit if no data
        if (times == null || times.length == 0) {
            return GF.createPolygon();
        }
        
        // Create binary mask for cells within cutoff
        boolean[][] mask = new boolean[height][width];
        boolean hasReachableCells = false;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (idx < times.length && times[idx] <= cutoff && times[idx] > 0) {
                    mask[y][x] = true;
                    hasReachableCells = true;
                }
            }
        }
        
        if (!hasReachableCells) {
            return GF.createPolygon();
        }
        
        // Run marching squares to extract contour segments
        List<LineSegment> segments = extractContourSegments(mask, width, height);
        
        if (segments.isEmpty()) {
            return GF.createPolygon();
        }
        
        // Stitch segments into closed rings
        List<LinearRing> rings = stitchSegmentsToRings(segments);
        
        if (rings.isEmpty()) {
            return GF.createPolygon();
        }
        
        // Convert grid coordinates to geographic coordinates and build polygons
        List<Polygon> polygons = new ArrayList<>();
        
        for (LinearRing ring : rings) {
            LinearRing geoRing = convertToGeographicCoordinates(ring, grid);
            if (geoRing != null && geoRing.getNumPoints() >= 4) {
                polygons.add(GF.createPolygon(geoRing));
            }
        }
        
        if (polygons.isEmpty()) {
            return GF.createPolygon();
        }
        
        // Union multiple polygons if needed
        if (polygons.size() == 1) {
            return polygons.get(0);
        } else {
            return UnaryUnionOp.union(polygons);
        }
    }
    
    /** Extract contour line segments using marching squares algorithm. */
    private static List<LineSegment> extractContourSegments(boolean[][] mask, int width, int height) {
        List<LineSegment> segments = new ArrayList<>();
        
        // Process each 2x2 cell in the grid
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                // Get the 4 corner values for this cell
                boolean tl = mask[y][x];         // top-left
                boolean tr = mask[y][x + 1];     // top-right  
                boolean bl = mask[y + 1][x];     // bottom-left
                boolean br = mask[y + 1][x + 1]; // bottom-right
                
                // Calculate marching squares case (0-15)
                int caseIndex = 0;
                if (tl) caseIndex |= 8;
                if (tr) caseIndex |= 4;
                if (br) caseIndex |= 2;
                if (bl) caseIndex |= 1;
                
                // Add line segments based on the case
                addSegmentsForCase(segments, caseIndex, x, y);
            }
        }
        
        return segments;
    }
    
    /** Add line segments for a specific marching squares case. */
    private static void addSegmentsForCase(List<LineSegment> segments, int caseIndex, int x, int y) {
        // Cell corners and edge midpoints
        double x0 = x, x1 = x + 1;
        double y0 = y, y1 = y + 1;
        double xMid = x + 0.5, yMid = y + 0.5;
        
        // Edge midpoints: top, right, bottom, left
        Coordinate top = new Coordinate(xMid, y0);
        Coordinate right = new Coordinate(x1, yMid);
        Coordinate bottom = new Coordinate(xMid, y1);
        Coordinate left = new Coordinate(x0, yMid);
        
        switch (caseIndex) {
            case 0:  // 0000 - no segments
            case 15: // 1111 - no segments
                break;
            case 1:  // 0001 - bottom-left corner
                segments.add(new LineSegment(left, bottom));
                break;
            case 2:  // 0010 - bottom-right corner
                segments.add(new LineSegment(bottom, right));
                break;
            case 3:  // 0011 - bottom edge
                segments.add(new LineSegment(left, right));
                break;
            case 4:  // 0100 - top-right corner
                segments.add(new LineSegment(top, right));
                break;
            case 5:  // 0101 - left and right edges (ambiguous case)
                segments.add(new LineSegment(left, top));
                segments.add(new LineSegment(bottom, right));
                break;
            case 6:  // 0110 - right edge
                segments.add(new LineSegment(top, bottom));
                break;
            case 7:  // 0111 - top-left corner (inverted)
                segments.add(new LineSegment(left, top));
                break;
            case 8:  // 1000 - top-left corner
                segments.add(new LineSegment(left, top));
                break;
            case 9:  // 1001 - left edge
                segments.add(new LineSegment(bottom, top));
                break;
            case 10: // 1010 - top and bottom edges (ambiguous case)
                segments.add(new LineSegment(left, bottom));
                segments.add(new LineSegment(top, right));
                break;
            case 11: // 1011 - top-right corner (inverted)
                segments.add(new LineSegment(top, right));
                break;
            case 12: // 1100 - top edge
                segments.add(new LineSegment(left, right));
                break;
            case 13: // 1101 - bottom-right corner (inverted)
                segments.add(new LineSegment(bottom, right));
                break;
            case 14: // 1110 - bottom-left corner (inverted)
                segments.add(new LineSegment(left, bottom));
                break;
        }
    }
    
    /** Stitch line segments into closed linear rings. */
    private static List<LinearRing> stitchSegmentsToRings(List<LineSegment> segments) {
        List<LinearRing> rings = new ArrayList<>();
        List<LineSegment> remaining = new ArrayList<>(segments);
        
        while (!remaining.isEmpty()) {
            List<Coordinate> ringCoords = new ArrayList<>();
            LineSegment current = remaining.remove(0);
            
            ringCoords.add(new Coordinate(current.p0));
            ringCoords.add(new Coordinate(current.p1));
            
            Coordinate target = current.p1;
            boolean foundConnection = true;
            
            // Try to build a closed ring
            while (foundConnection && !target.equals2D(ringCoords.get(0))) {
                foundConnection = false;
                
                for (int i = 0; i < remaining.size(); i++) {
                    LineSegment seg = remaining.get(i);
                    
                    if (target.equals2D(seg.p0)) {
                        ringCoords.add(new Coordinate(seg.p1));
                        target = seg.p1;
                        remaining.remove(i);
                        foundConnection = true;
                        break;
                    } else if (target.equals2D(seg.p1)) {
                        ringCoords.add(new Coordinate(seg.p0));
                        target = seg.p0;
                        remaining.remove(i);
                        foundConnection = true;
                        break;
                    }
                }
            }
            
            // Close the ring if we have enough points
            if (ringCoords.size() >= 3) {
                if (!ringCoords.get(0).equals2D(ringCoords.get(ringCoords.size() - 1))) {
                    ringCoords.add(new Coordinate(ringCoords.get(0)));
                }
                
                if (ringCoords.size() >= 4) {
                    try {
                        LinearRing ring = GF.createLinearRing(ringCoords.toArray(new Coordinate[0]));
                        if (ring.isValid()) {
                            rings.add(ring);
                        }
                    } catch (Exception e) {
                        // Skip invalid rings
                    }
                }
            }
        }
        
        return rings;
    }
    
    /** Convert grid coordinates to geographic coordinates. */
    private static LinearRing convertToGeographicCoordinates(LinearRing ring, Grid grid) {
        Coordinate[] coords = ring.getCoordinates();
        Coordinate[] geoCoords = new Coordinate[coords.length];
        
        for (int i = 0; i < coords.length; i++) {
            double gridX = coords[i].x;
            double gridY = coords[i].y;
            
            // Convert from local grid coordinates to absolute pixel coordinates
            double absoluteX = gridX + grid.extents.west;
            double absoluteY = gridY + grid.extents.north;
            
            // Convert to geographic coordinates using Grid's static methods
            double lon = Grid.pixelToLon(absoluteX, grid.extents.zoom);
            double lat = Grid.pixelToLat(absoluteY, grid.extents.zoom);
            
            geoCoords[i] = new Coordinate(lon, lat);
        }
        
        try {
            return GF.createLinearRing(geoCoords);
        } catch (Exception e) {
            return null;
        }
    }
}
