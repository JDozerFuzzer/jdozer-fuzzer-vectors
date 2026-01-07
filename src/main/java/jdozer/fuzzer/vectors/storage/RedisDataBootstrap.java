/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.storage;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.quarkus.logging.Log;
import jdozer.fuzzer.vectors.storage.entity.Vector;
import redis.clients.jedis.Jedis;

public class RedisDataBootstrap {

    private static String INIT_DATA_PATH = "vectors-v2.json";
    private Gson gson = new Gson();
    private Jedis jedis;
    private final KeyManager keyManager = new KeyManager();

    public RedisDataBootstrap(Jedis jedis) {
        this.jedis = jedis;
    }

    public void init() {
        try {

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(INIT_DATA_PATH);
            if (inputStream == null) {
                throw new NoSuchFileException("Initialization file not found: " + INIT_DATA_PATH);
            }

            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Vector>>() {
            }.getType();
            List<Vector> vectors = gson.fromJson(json, listType);

            for (Vector vector : vectors) {
                Integer id = vector.getId();
                String value = gson.toJson(vector);
                String key = this.keyManager.forVec(id);
                if (!this.jedis.exists(key)) {
                    this.jedis.set(key, value);
                }
            }

            Log.info("Data loaded into Redis successfully!");
            Log.info("from: " + INIT_DATA_PATH);

        } catch (NoSuchFileException e) {
            Log.error(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.error("Error loading data into Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
