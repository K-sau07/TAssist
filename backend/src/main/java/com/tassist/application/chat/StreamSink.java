package com.tassist.application.chat;

import java.util.Map;

/**
 * Transport-agnostic sink for §11.6 SSE events. The web layer adapts this onto an SseEmitter;
 * tests adapt it onto an in-memory recorder. Event names + payload keys match §11.6 exactly.
 */
public interface StreamSink {
    /** Emit a named event with a JSON-serializable payload. */
    void emit(String event, Map<String, Object> data);

    /** Signal the stream is fully done (close transport). */
    void complete();

    /** Signal a transport-level failure (close transport with error). */
    void fail(Throwable t);
}
