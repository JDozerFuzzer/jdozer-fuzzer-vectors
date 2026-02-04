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
public class SeederSuccessfulPayload {

    private UUID fuzzerId;

}
