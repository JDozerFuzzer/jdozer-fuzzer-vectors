package jdozer.fuzzer.vectors.async.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeederSuccessful {

    private Headers headers;
    private SeederSuccessfulPayload payload;

}
