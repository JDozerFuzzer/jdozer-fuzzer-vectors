/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.dto;

import java.util.List;

import jdozer.fuzzer.vectors.storage.entity.Vector;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationParameter {

    private String context;
    private List<String> pathStrAttributes;
    private List<Vector> vectors;
    private String dummy;
    
}
