package jdozer.fuzzer.vectors.async.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VectorsEvent {

    private Headers headers;
    private Object payload;

}
