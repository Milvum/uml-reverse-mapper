package com.milvum.urm;

import com.milvum.urm.DomainClassFinder;
import net.sf.qualitytest.CoverageForPrivateConstructor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DomainClassFinderTest {

    @Before
    public void setup() {
        DomainClassFinder.ALLOW_FINDING_INTERNAL_CLASSES = true;
    }

    @Test
    public void withSinglePackage() throws Exception {
        List<Class<?>> classes = findClasses("com.milvum.urm.testdomain.person");
        assertThat(classes.size(), is(4));
    }

    @Test
    public void withSinglePackageAndIgnore() throws Exception {
        List<Class<?>> classes = DomainClassFinder.findClasses(Arrays.asList("com.milvum.urm.testdomain.person"),
                Arrays.asList("Manager"), null);
        assertThat(classes.size(), is(3));
    }

    @Test
    public void withMultiPackages() throws Exception {
        List<Class<?>> classes = findClasses("com.milvum.urm.testdomain.person", "com.milvum.urm.testdomain.another");
        assertThat(classes.size(), is(5));
    }

    @Test
    public void withMultiPackagesAndIgnores() throws Exception {
        List<Class<?>> classes = DomainClassFinder.findClasses(Arrays.asList("com.milvum.urm.testdomain.person", "Another"),
                Arrays.asList("Manager", "Another"), null);
        assertThat(classes.size(), is(3));
    }

    @Test
    public void filtersAnonymousClasses() throws Exception {
        List<Class<?>> classes = findClasses("com.milvum.urm.testdomain.withanonymousclass");
        assertThat(classes.size(), is(1));
    }

    @Test
    public void privateConstructor() {
        CoverageForPrivateConstructor.giveMeCoverage(DomainClassFinder.class);
    }

    private List<Class<?>> findClasses(String... packages) {
        return DomainClassFinder.findClasses(Arrays.asList(packages), new ArrayList<>(), null);
    }
}
