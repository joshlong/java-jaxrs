package io.opentracing.contrib.jaxrs.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs.internal.SpanWrapper;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public class SpanServerRequestFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(SpanServerRequestFilter.class.getName());

    public static final String SPAN_PROP_ID = SpanServerRequestFilter.class.getName() + ".currentServerSpan";

    private Tracer tracer;
    private String operationName;
    private List<ServerSpanDecorator> spanDecorators;

    public SpanServerRequestFilter(Tracer tracer, String operationName,
                                   List<ServerSpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // return in case filter if registered twice
        if (requestContext.getProperty(SPAN_PROP_ID) != null) {
            return;
        }

        if (tracer != null) {
            SpanContext extractedSpanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new ServerHeadersExtractTextMap(requestContext.getHeaders()));

            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(requestContext.getMethod())
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);

            if (extractedSpanContext != null) {
                spanBuilder.asChildOf(extractedSpanContext);
            }

            Span span = spanBuilder.start();

            if (spanDecorators != null) {
                for (ServerSpanDecorator decorator: spanDecorators) {
                    decorator.decorateRequest(requestContext, span);
                }
            }

            // override operation name set by @Traced
            if (this.operationName != null) {
                span.setOperationName(operationName);
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Creating server span: " + operationName);
            }

            requestContext.setProperty(SPAN_PROP_ID, new SpanWrapper(span));
        }
    }
}
