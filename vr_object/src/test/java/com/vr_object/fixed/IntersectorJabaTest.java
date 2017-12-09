package com.vr_object.fixed;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Michael Lukin on 15.11.2017.
 */

public class IntersectorJabaTest {
    @Test
    public void testAddSagittaX01() {
        Intersector intersector = new Intersector(0.4f, 5);
        intersector.addSagitta(new Float[]{0f, 0f, 0f}, new Float[]{2f, 0f, 0f});

        HashMap space = intersector.getSpace();
        Intersector.Coordinate c0 = new Intersector.Coordinate(0, 0, 0);
        assertTrue(space.get(c0) != null);

        c0 = new Intersector.Coordinate(1, 0, 0);
        assertTrue(space.get(c0).equals(1));

        c0 = new Intersector.Coordinate(2, 0, 0);
        assertTrue(space.get(c0).equals(1));
    }

    @Test
    public void testAddSagittaX02() {
        Intersector intersector = new Intersector(0.4f, 5);
        intersector.addSagitta(new Float[]{0f, 0f, 0f}, new Float[]{3f, 1f, 0f});

        HashMap space = intersector.getSpace();
        Intersector.Coordinate c0 = new Intersector.Coordinate(0, 0, 0);
        assertTrue(space.get(c0) != null);

        c0 = new Intersector.Coordinate(1, 0, 0);
        assertTrue(space.get(c0).equals(1));

        c0 = new Intersector.Coordinate(2, 1, 0);
        assertTrue(space.get(c0).equals(1));

        c0 = new Intersector.Coordinate(3, 1, 0);
        assertTrue(space.get(c0).equals(1));

        intersector.clear();
        assertNull(space.get(c0));
    }

    @Test
    public void testAddSagittaY02() {
        Intersector intersector = new Intersector(0.4f, 5);
        intersector.addSagitta(new Float[]{0f, 0f, 0f}, new Float[]{1f, 3f, 0f});

        HashMap space = intersector.getSpace();
        Intersector.Coordinate c0 = new Intersector.Coordinate(0, 0, 0);
        assertTrue(space.get(c0) != null);

        c0 = new Intersector.Coordinate(0, 1, 0);
        assertTrue(space.get(c0).equals(1));

        assertNull(space.get(new Intersector.Coordinate(0, 2, 0)));

        c0 = new Intersector.Coordinate(1, 2, 0);
        assertTrue(space.get(c0).equals(1));

        c0 = new Intersector.Coordinate(1, 3, 0);
        assertTrue(space.get(c0).equals(1));
    }

    @Test
    public void testAddSagittaZ02() {
        Intersector intersector = new Intersector(0.4f, 5);
        intersector.addSagitta(new Float[]{0f, 0f, 0f}, new Float[]{1f, 0f, 3f});

        HashMap space = intersector.getSpace();

        assertEquals(space.get(new Intersector.Coordinate(0, 0, 0)), 1);
        assertEquals(space.get(new Intersector.Coordinate(0, 0, 1)), 1);
        assertEquals(space.get(new Intersector.Coordinate(1, 0, 2)), 1);
        assertEquals(space.get(new Intersector.Coordinate(1, 0, 3)), 1);

        assertNull(space.get(new Intersector.Coordinate(1, 0, 0)));
    }
}
