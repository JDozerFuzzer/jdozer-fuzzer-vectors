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
import jdozer.fuzzer.vectors.storage.RedisService;

@ApplicationScoped
public class EventService {

    @Inject
    private MessageMediator messageMediator;
    @Inject
    private RedisService redisService;

    private static final String IN_ENTITY_TYPE = "fuzzer-core";
    private static final String IN_EVENT_TYPE_SUCCESS = "build-success";

    public void inbound(String event) {
        Gson gson = new Gson();
        try {
            Message message = gson.fromJson(event, Message.class);
            if (IN_ENTITY_TYPE.equals(message.getEntityType())
                    && IN_EVENT_TYPE_SUCCESS.equals(message.getEventType())) {
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

    private String encode(Object data) {
        Base64.Encoder encoder = Base64.getEncoder();
        String dataJson = new Gson().toJson(data);
        return new String(encoder.encodeToString(dataJson.getBytes()));
    }

    @Inject
    @ConfigProperty(name = "jdozer.fuzzer.vectors.redis.stream.name")
    private String CHANNEL;

    private static final String ENTITY_TYPE = "fuzzer-vectors";
    public enum EventType {

        LOAD_SUCCESS("load-success"),
        LOAD_FAILURE("load-failure");

        private String name;

        private EventType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
