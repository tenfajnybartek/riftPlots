package pl.riftplots;

import org.junit.jupiter.api.Test;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.util.BoundingBoxUtil;

import static org.junit.jupiter.api.Assertions.*;

class PlotBoundsTest {

    @Test
    void testAreaAndDimensions() {
        PlotBounds bounds = new PlotBounds("world", 10, 60, 20, 19, 79, 29);
        assertEquals(10, bounds.getWidthX());
        assertEquals(20, bounds.getHeightY());
        assertEquals(10, bounds.getLengthZ());
        assertEquals(100L, bounds.getHorizontalArea());
        assertEquals(2000L, bounds.getVolume());
    }

    @Test
    void testCoordinatesNormalization() {
        PlotBounds bounds = new PlotBounds("world", 20, 80, 30, 10, 60, 15);
        assertEquals(10, bounds.getMinX());
        assertEquals(20, bounds.getMaxX());
        assertEquals(60, bounds.getMinY());
        assertEquals(80, bounds.getMaxY());
        assertEquals(15, bounds.getMinZ());
        assertEquals(30, bounds.getMaxZ());
    }

    @Test
    void testContains() {
        PlotBounds bounds = new PlotBounds("world", 0, 50, 0, 10, 100, 10);
        assertTrue(bounds.contains("world", 5, 60, 5));
        assertTrue(bounds.contains("world", 0, 50, 0));
        assertTrue(bounds.contains("world", 10, 100, 10));
        assertFalse(bounds.contains("world", 11, 60, 5));
        assertFalse(bounds.contains("other_world", 5, 60, 5));
    }

    @Test
    void testIntersects() {
        PlotBounds a = new PlotBounds("world", 0, 0, 0, 10, 10, 10);
        PlotBounds b = new PlotBounds("world", 5, 5, 5, 15, 15, 15);
        PlotBounds c = new PlotBounds("world", 20, 20, 20, 30, 30, 30);
        PlotBounds d = new PlotBounds("nether", 0, 0, 0, 10, 10, 10);

        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertFalse(a.intersects(c));
        assertFalse(a.intersects(d));
    }

    @Test
    void testAdjacent() {
        PlotBounds a = new PlotBounds("world", 0, 0, 0, 10, 10, 10);
        PlotBounds touching = new PlotBounds("world", 11, 0, 0, 20, 10, 10);
        assertFalse(a.intersects(touching));
        assertTrue(BoundingBoxUtil.isAdjacentOrTouching(a, touching));
        assertFalse(BoundingBoxUtil.isAdjacentOrTouching(a, new PlotBounds("world", 11, 0, 100, 20, 10, 110)));
        assertFalse(BoundingBoxUtil.isAdjacentOrTouching(a, new PlotBounds("world", 11, 100, 0, 20, 110, 10)));
    }
}
