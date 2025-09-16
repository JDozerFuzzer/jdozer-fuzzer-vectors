/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors;

import lombok.Getter;

@Getter
public class VectorException extends Exception {

    private String message;
    private String detail;

    public VectorException(String message) {
        super(message);
        this.message = message;
    }

    public VectorException(String message, String detail) {
        super();
        this.message = message;
        this.detail = detail;
    }

}
