// com/conveyal/r5/analyst/TravelTimeSurface.java
package com.conveyal.r5.analyst;

import org.locationtech.jts.geom.Geometry;
import java.util.Arrays;
import java.util.List;

public final class TravelTimeSurface {
    public final Grid grid;
    /** Travel time in SECONDS for each cell; same row-major order as grid extents. */
    public final int[] timesSeconds;

    public TravelTimeSurface(Grid grid, int[] timesSeconds) {
        if (timesSeconds == null) throw new IllegalArgumentException("timesSeconds is null");
        if (grid == null) throw new IllegalArgumentException("grid is null");
        int expected = grid.extents.width * grid.extents.height;
        if (timesSeconds.length != expected) {
            throw new IllegalArgumentException("times length " + timesSeconds.length +
                    " != grid size " + expected);
        }
        this.grid = grid;
        this.timesSeconds = timesSeconds;
    }

    /** Build polygons for the given cutoff(s) in seconds. */
    public List<Geometry> contour(int... cutoffsSeconds) {
        // Optionally normalize unreached values (e.g., 0 or Integer.MAX_VALUE) here:
        int[] t = timesSeconds;
        // If your reducer marks unreached as <=0, leave IsochroneBuilder to filter.
        return IsochroneBuilder.contour(grid, t, cutoffsSeconds);
    }
}
