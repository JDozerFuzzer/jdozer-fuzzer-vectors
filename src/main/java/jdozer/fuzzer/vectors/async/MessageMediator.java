/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.async;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.VectorsService;
import jdozer.fuzzer.vectors.async.EventService.EventType;
import jdozer.fuzzer.vectors.async.event.ErrorPayload;
import jdozer.fuzzer.vectors.async.event.SeederSuccessful;
import jdozer.fuzzer.vectors.async.event.VectorsSuccessfulPayload;
import lombok.AllArgsConstructor;

@ApplicationScoped
@AllArgsConstructor
public class MessageMediator {

    @Inject
    private EventService eventService;
    @Inject
    private VectorsService vectorsService;

    public void processMessage(SeederSuccessful message) {
        try {

            // byte[] data = Base64.getDecoder().decode(message.getData());
            // String dataString = new String(data);
            // JsonNode jsonNode = (new ObjectMapper()).readTree(dataString);
            // UUID fuzzerId = UUID.fromString(jsonNode.get("id").asText());
            this.vectorsService.createVectors(message.getPayload().getFuzzerId());

            VectorsSuccessfulPayload vectorsSuccessfulPayload = new VectorsSuccessfulPayload();
            vectorsSuccessfulPayload.setFuzzerId(message.getPayload().getFuzzerId());

            this.eventService.outbound(EventType.BUILDER_SUCCESSFUL, vectorsSuccessfulPayload,
                    message.getPayload().getFuzzerId());

        } catch (Exception e) {
            Log.error("Error processing message: " + e.getMessage());
            ErrorPayload errorPayload = new ErrorPayload();
            errorPayload.setFuzzerId(message.getPayload().getFuzzerId());
            errorPayload.setMessage("Error to add vectors: " + e.getMessage());
            errorPayload.setDetails(e.toString());
            errorPayload.setAction("continue");
            this.eventService.outbound(EventType.ERROR, errorPayload, message.getHeaders().getEntityId());
            e.printStackTrace();
        }
    }

}
