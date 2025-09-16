/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.storage;

import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.storage.entity.Vector;

@ApplicationScoped
public class StorageService {

    @Inject
    private RedisService redisService;
    private final KeyManager keyManager = new KeyManager();

    public StorageService() {
    }

    public String findFuzzer(UUID fuzzerId) {
        return this.redisService.get(this.keyManager.forFuzz(fuzzerId));
    }

    public String findOperation(String operationName, UUID fuzzerId) {
        return this.redisService.get(this.keyManager.forOperation(operationName, fuzzerId));
    }

    public List<String> getContextParameters(UUID fuzzerId, String operationName) {
        return asList("payload", "headers", "path", "query").stream().map(v -> {
            return (this.redisService.keys(this.keyManager.operationPrefix(operationName, v, fuzzerId)).length > 0 ? v
                    : null);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Integer getDummyCant(UUID fuzzerId, String operationName, String property) {
        return this.redisService.keys(this.keyManager.operationPrefix(operationName, property, fuzzerId)).length;
    }

    public String getDummyValid(UUID fuzzerId, String operationName, String property) {
        String[] keys = this.redisService.keys(this.keyManager.operationPrefix(operationName, property, fuzzerId));
        for (String k : keys) {
            String json = this.redisService.get(k);
            Boolean valid = JsonPath.read(json, "$.valid");
            if (valid) {
                return json;
            }
        }
        Log.error("No valid dummy found for " + operationName + ":" + property + "!");
        throw new RuntimeException("No valid dummy found!");
    }

    public String getOperationsName(String operation, UUID fuzzerId) {
        String op = this.findOperation(operation, fuzzerId);
        return JsonPath.read(op, "$.name");
    }

    public void save(String key, String value) {
        this.redisService.set(key, value);
    }

    public List<Vector> getRndFuzzVector(int cant) {

        if (cant <= 0) {
            Log.warn("getRndFuzzVector: No amount is requested: " + cant);
            return Collections.emptyList();
        }

        String[] keys = this.redisService.keys(this.keyManager.vecPrefix());
        if (keys.length == 0) {
            Log.error("getRndFuzzVector: No vectors found!");
            return Collections.emptyList();
        }

        try {
            List<Vector> fuzzVectors = new ArrayList<>();
            Gson gson = new Gson();
            String json;
            Vector vector;
            int randomIndex;
            for (int i = 0; i < cant; i++) {
                randomIndex = this.random(0, keys.length - 1);
                json = this.redisService.get(keys[randomIndex]);
                vector = gson.fromJson(json, Vector.class);
                fuzzVectors.add(vector);
            }
            return fuzzVectors;
        } catch (Exception e) {
            Log.error("getRndFuzzVector: " + e.getMessage());
            throw new RuntimeException("getRndFuzzVector failed!");
        }

    }

    private int random(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }

}
