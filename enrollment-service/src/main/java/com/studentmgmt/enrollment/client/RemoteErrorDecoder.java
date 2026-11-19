package com.studentmgmt.enrollment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Turns HTTP 404 and 409 from the other services into typed exceptions that carry the remote
 * message. Everything else (5xx, timeouts) falls through to Feign's default decoder and is
 * treated as an outage by the fallback factories.
 */
@Slf4j
public class RemoteErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder fallback = new ErrorDecoder.Default();

    public RemoteErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status == 404) {
            return new RemoteNotFoundException(extractMessage(response));
        }
        if (status == 409) {
            return new RemoteConflictException(extractMessage(response));
        }
        return fallback.decode(methodKey, response);
    }

    private String extractMessage(Response response) {
        if (response.body() != null) {
            try (InputStream in = response.body().asInputStream()) {
                JsonNode message = objectMapper.readTree(in).get("message");
                if (message != null && !message.isNull()) {
                    return message.asText();
                }
            } catch (IOException e) {
                log.debug("Could not read error body from remote service", e);
            }
        }
        return "Remote service returned HTTP " + response.status();
    }
}
