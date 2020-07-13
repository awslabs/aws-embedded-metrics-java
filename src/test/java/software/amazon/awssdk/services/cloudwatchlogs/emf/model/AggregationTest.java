package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AggregationTest {
    @Test
    public void testEquals() {
        Aggregation agg1 = new Aggregation("d1", "d2", "d3");
        Aggregation agg2 = new Aggregation("d1", "d2", "d3");
        Aggregation agg3 = new Aggregation("d2", "d3");
        Aggregation agg4 = new Aggregation("d1", "d3");
        Aggregation agg5 = new Aggregation("d1", "d2");
        Aggregation agg6 = new Aggregation("d1");
        Aggregation agg7 = new Aggregation("d2");
        Aggregation agg8 = new Aggregation("d3");
        Aggregation agg9 = new Aggregation("d3", "d1", "d2");
        Aggregation agg10 = new Aggregation("d1", "d2", "d4");
        Integer i = 9;

        assertEquals(agg1, agg1);
        assertEquals(agg1, agg2);
        assertNotEquals(agg1, agg3);
        assertNotEquals(agg1, agg4);
        assertNotEquals(agg1, agg5);
        assertNotEquals(agg1, agg6);
        assertNotEquals(agg1, agg7);
        assertNotEquals(agg1, agg8);
        assertEquals(agg1, agg9);
        assertNotEquals(agg1, agg10);
        assertNotEquals(agg1, null);
        assertNotEquals(agg1, i);
    }
}
