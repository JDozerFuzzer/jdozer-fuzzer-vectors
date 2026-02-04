package jdozer.fuzzer.vectors.async.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Headers {

    private UUID id;
    private Long timestamp;
    private String version;
    private UUID entityId;
    private String entityType;
    private String eventType;

}
