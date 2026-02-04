package jdozer.fuzzer.vectors.async.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VectorCreated {

    private UUID fuzzerId;
    private String vectorId;
    private String operationId;
    private String context;

}
