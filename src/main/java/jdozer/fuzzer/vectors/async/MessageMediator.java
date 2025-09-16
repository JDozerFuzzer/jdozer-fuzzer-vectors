/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.async;

import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.VectorsService;
import jdozer.fuzzer.vectors.async.EventService.EventType;
import lombok.AllArgsConstructor;

@ApplicationScoped
@AllArgsConstructor
public class MessageMediator {

    @Inject
    private EventService eventService;
    @Inject
    private VectorsService vectorsService;

    public void processMessage(Message message) {
        try {

            byte[] data = Base64.getDecoder().decode(message.getData());
            String dataString = new String(data);
            JsonNode jsonNode = (new ObjectMapper()).readTree(dataString);
            UUID fuzzerId = UUID.fromString(jsonNode.get("id").asText());
            this.vectorsService.createVectors(fuzzerId);

            EventData eventData = new EventData();
            eventData.setFuzzerId(fuzzerId);
            eventData.setOwner(UUID.fromString(jsonNode.get("owner").asText()));
            eventData.setMessages("Weapon is loaded!");

            this.eventService.outbound(EventType.LOAD_SUCCESS, eventData, fuzzerId, message.getTraceId());

        } catch (Exception e) {
            Log.error("Error processing message: " + e.getMessage());
            EventException eventException = new EventException("Error processing data.", e.getMessage());
            this.eventService.outbound(EventType.LOAD_FAILURE, eventException, message.getEntityId(),
                    message.getTraceId());
            e.printStackTrace();
        }
    }

}
