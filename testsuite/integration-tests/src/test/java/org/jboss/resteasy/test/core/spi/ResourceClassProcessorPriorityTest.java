package org.jboss.resteasy.test.core.spi;

import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.test.core.spi.resource.ResourceClassProcessorPriiorityAImplementation;
import org.jboss.resteasy.test.core.spi.resource.ResourceClassProcessorPriiorityBImplementation;
import org.jboss.resteasy.test.core.spi.resource.ResourceClassProcessorPriiorityCImplementation;
import org.jboss.resteasy.test.core.spi.resource.ResourceClassProcessorPureEndPoint;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.testing.tools.deployments.DeploymentDescriptors;

/**
 * @tpSubChapter ResourceClassProcessor SPI
 * @tpChapter Integration tests
 * @tpTestCaseDetails ResourceClassProcessor and Priority annotation test
 * @tpSince RESTEasy 3.6
 */
@ExtendWith(ArquillianExtension.class)
public class ResourceClassProcessorPriorityTest {

    protected static final Logger logger = Logger.getLogger(ResourceClassProcessorPriorityTest.class.getName());

    private static List<String> visitedProcessors = new ArrayList<>();

    public static synchronized void addToVisitedProcessors(String item) {
        visitedProcessors.add(item);
    }

    static ResteasyClient client;

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = TestUtil.prepareArchive(ResourceClassProcessorPriorityTest.class.getSimpleName());
        war.addClass(ResourceClassProcessorPriorityTest.class);
        war.addClass(PortProviderUtil.class);
        war.addAsManifestResource(DeploymentDescriptors.createPermissionsXmlAsset(
                new SocketPermission(PortProviderUtil.getHost(), "connect,resolve"),
                new PropertyPermission("org.jboss.resteasy.port", "read"),
                new PropertyPermission("quarkus.tester", "read"),
                new RuntimePermission("getenv.RESTEASY_PORT"),
                new PropertyPermission("ipv6", "read"),
                new PropertyPermission("node", "read"),
                new PropertyPermission("arquillian.*", "read"),
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks")), "permissions.xml");
        return TestUtil.finishContainerPrepare(war, null,
                ResourceClassProcessorPureEndPoint.class,
                ResourceClassProcessorPriiorityAImplementation.class,
                ResourceClassProcessorPriiorityBImplementation.class,
                ResourceClassProcessorPriiorityCImplementation.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceClassProcessorPriorityTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Deployment uses three ResourceClassProcessors with Priority annotation,
     *                this priority annotation should be used by RESTEasy
     * @tpSince RESTEasy 3.6
     */
    @Test
    public void priorityTest() {
        // init client
        client = (ResteasyClient) ClientBuilder.newClient();

        // do request
        Response response = client.target(generateURL("/pure/pure")).request().get();
        Assertions.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());

        // log visited processors
        int i = 0;
        for (String item : visitedProcessors) {
            logger.info(String.format("%d. %s", ++i, item));
        }

        // asserts
        Assertions.assertTrue(visitedProcessors.size() >= 3);
        Assertions.assertEquals("A", visitedProcessors.get(0));
        Assertions.assertEquals("C", visitedProcessors.get(1));
        Assertions.assertEquals("B", visitedProcessors.get(2));

        // close client
        client.close();
    }
}
