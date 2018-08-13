package com.milvum.urm.domain;

import com.milvum.urm.domain.Direction;
import com.milvum.urm.domain.EdgeType;
import com.milvum.urm.domain.Edge;
import com.milvum.urm.domain.DomainClass;
import com.milvum.urm.testdomain.person.Manager;
import com.milvum.urm.testdomain.person.Person;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class EdgeTest {

    @Test
    public void testEqualsContract() throws Exception {
        EqualsVerifier.forClass(Edge.class).verify();
    }

    @Test
    public void toStringWorks() {
        String toString = new Edge(new DomainClass(Person.class), new DomainClass(Manager.class), EdgeType.ONE_TO_ONE, Direction.BI_DIRECTIONAL).toString();
        assertThat(toString, containsString("source"));
        assertThat(toString, containsString("target"));
    }

}
