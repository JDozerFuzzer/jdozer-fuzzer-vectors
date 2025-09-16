/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.async;

import jdozer.fuzzer.vectors.VectorException;

public class EventException extends VectorException {

    public EventException(String message) {
        super(message);
    }

    public EventException(String message, String detail) {
        super(message, detail);
    }
    
}
