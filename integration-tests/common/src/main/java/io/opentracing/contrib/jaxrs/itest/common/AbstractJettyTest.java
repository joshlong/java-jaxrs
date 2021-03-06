package io.opentracing.contrib.jaxrs.itest.common;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;

import io.opentracing.contrib.jaxrs.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs.server.ServerTracingDynamicFeature;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.TextMapPropagator;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractJettyTest {

    public static final String TRACER_BUILDER_ATTRIBUTE = "tracerBuilder";
    public static final String CLIENT_BUILDER_ATTRIBUTE = "clientBuilder";

    public static int SERVER_PORT = 3000;

    protected Server jettyServer;
    protected MockTracer mockTracer;
    protected Client client;

    protected abstract void initServletContext(ServletContextHandler context);

    protected void initTracing(ServletContextHandler context) {
        ClientTracingFeature.Builder clientTracingBuilder = ClientTracingFeature.Builder
                .traceAll(mockTracer, client);

        ServerTracingDynamicFeature.Builder serverTracingBuilder = ServerTracingDynamicFeature.Builder
                .traceAll(mockTracer);

        context.setAttribute(CLIENT_BUILDER_ATTRIBUTE, clientTracingBuilder);
        context.setAttribute(TRACER_BUILDER_ATTRIBUTE, serverTracingBuilder);
    }

    @Before
    public void before() throws Exception {
        mockTracer = new MockTracer(new TextMapPropagator());
        client = ClientBuilder.newBuilder().build();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        initServletContext(context);
        initTracing(context);

        jettyServer = new Server(SERVER_PORT);
        jettyServer.setHandler(context);
        jettyServer.start();
    }

    @After
    public void after() throws Exception {
        jettyServer.stop();
    }

    public String url(String path) {
        return "http://localhost:" + SERVER_PORT + path;
    }
}
