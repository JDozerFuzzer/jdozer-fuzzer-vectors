/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.storage;

import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.enterprise.context.ApplicationScoped;
import redis.clients.jedis.UnifiedJedis;

@ApplicationScoped
public class RedisService {

    private String REDIS_URI = ConfigProvider.getConfig().getValue("jdozer.fuzzer.vectors.redis.uri", String.class);

    private UnifiedJedis jedis;
    

    public RedisService() {
        this.jedis = new UnifiedJedis(this.REDIS_URI);
    }

    public void set(String key, String value) {
        jedis.set(key, value);
    }

    public String get(String key) {
        return jedis.get(key);
    }

    public String[] keys(String prefix) {
        return jedis.keys(prefix).toArray(new String[0]);
    }

    public void close() {
        jedis.close();
    }

    public void publish(String channel, String message) {
        jedis.publish(channel, message);
    }    


}
