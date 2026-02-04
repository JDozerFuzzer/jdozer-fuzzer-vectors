/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.async;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.gson.Gson;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.async.event.Headers;
import jdozer.fuzzer.vectors.async.event.SeederSuccessful;
import jdozer.fuzzer.vectors.async.event.VectorsEvent;
import jdozer.fuzzer.vectors.storage.RedisService;

@ApplicationScoped
public class EventService {

    @Inject
    private MessageMediator messageMediator;
    @Inject
    private RedisService redisService;

    private static final String SEEDER_ENTITY_TYPE = "fuzzer-seeder";
    private static final String SEEDER_EVENT_TYPE_SUCCESS = "builder-successful";

    public void inbound(String event) {
        Gson gson = new Gson();
        try {
            SeederSuccessful message = gson.fromJson(event, SeederSuccessful.class);
            if (SEEDER_ENTITY_TYPE.equals(message.getHeaders().getEntityType())
                    && SEEDER_EVENT_TYPE_SUCCESS.equals(message.getHeaders().getEventType())) {
                messageMediator.processMessage(message);
            } else {
                Log.debug("Ignoring message: " + message.toString());
            }
        } catch (Exception e) {
            Log.error("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void outbound(EventType eventType, Object data, UUID entityId, UUID traceId) {

        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setTimestamp(Instant.now().getEpochSecond());
        message.setTraceId(traceId);
        message.setEntityId(entityId);
        message.setEntityType(ENTITY_TYPE);
        message.setEventType(eventType.getName());
        message.setData(this.encode(data));

        this.redisService.publish(this.CHANNEL, new Gson().toJson(message));

    }

    public void outbound(EventType eventType, Object payload, UUID entityId) {

        Headers headers = new Headers();
        headers.setId(UUID.randomUUID());
        headers.setTimestamp(Instant.now().getEpochSecond());
        headers.setVersion("1.0.0");
        headers.setEntityId(entityId);
        headers.setEntityType(ENTITY_TYPE);
        headers.setEventType(eventType.getName());

        VectorsEvent vectorsEvent = new VectorsEvent();
        vectorsEvent.setHeaders(headers);
        vectorsEvent.setPayload(payload);

        this.redisService.publish(this.CHANNEL, new Gson().toJson(vectorsEvent));

    }

    private String encode(Object data) {
        Base64.Encoder encoder = Base64.getEncoder();
        String dataJson = new Gson().toJson(data);
        return new String(encoder.encodeToString(dataJson.getBytes()));
    }

    @Inject
    @ConfigProperty(name = "jdozer.fuzzer.vectors.channels.vectors")
    private String CHANNEL;

    private static final String ENTITY_TYPE = "fuzzer-vectors";

    public enum EventType {

        BUILDER_SUCCESSFUL("builder-successful"),
        LOAD_SUCCESS("load-success"),
        LOAD_FAILURE("load-failure"),
        VECTOR_CREATED("vector-created"),
        ERROR("error");

        private String name;

        private EventType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
