package com.github.blockjon.flaptastic;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MainTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MainTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(MainTest.class );
    }

    public void testPassingAssertion()
    {
        assertTrue( true );
    }
    public void testFailingAssertion()
    {
        assertTrue( false );
    }
}
