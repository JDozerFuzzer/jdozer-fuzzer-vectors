/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.storage;

import java.util.UUID;

public class KeyManager {

    private static final String JDF = "JDF";

    public String forFuzz(UUID id) {
        return JDF.concat(":").concat(id.toString());
    }

    public String forVec(Integer id) {
        return JDF.concat(":VEC:").concat(id.toString());
    }

    public String vecPrefix() {
        return JDF.concat(":VEC:").concat("*");
    }

    public String forOperation(String operationName, UUID fuzzerId) {
        return forFuzz(fuzzerId).concat(":OP:").concat(operationName);
    }

    public String operationPrefix(String operationName, String context, UUID fuzzerId) {
        return forFuzz(fuzzerId).concat(":DMM:").concat(operationName).concat(":").concat(context).concat(":*");
    }

    public String forFake(UUID fuzzerId, String operationName, String context, UUID fakeId) {
        return forFuzz(fuzzerId).concat(":DMM:").concat(operationName).concat(":").concat(context).concat(":")
                .concat(fakeId.toString());
    }

}
