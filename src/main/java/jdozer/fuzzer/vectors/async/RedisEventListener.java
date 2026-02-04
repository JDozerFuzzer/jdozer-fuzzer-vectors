/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.async;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.gson.Gson;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.async.event.SeederSuccessful;
import jdozer.fuzzer.vectors.storage.RedisDataBootstrap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

@ApplicationScoped
@Startup
public class RedisEventListener {

    @Inject
    private MessageMediator messageMediator;
    private final String uri;
    private Jedis listenerJedis;
    private Thread listenerThread;
    private final String channel;

    private static final String ENTITY_TYPE = "fuzzer-seeder";
    private static final String EVENT_TYPE = "builder-successful";

    public RedisEventListener(
            @ConfigProperty(name = "jdozer.fuzzer.vectors.redis.uri") String uri,
            @ConfigProperty(name = "jdozer.fuzzer.vectors.channels.seeder") String channel) {
        this.uri = uri;
        this.channel = channel;
    }

    @PostConstruct
    public void init() {
        this.listenerJedis = new Jedis(uri);
        this.initializeData();
        this.startListening();
    }

    private void startListening() {
        Log.info("Initializing Redis listener for channel: " + channel);
        listenerThread = new Thread(() -> {
            try {
                JedisPubSub pubSub = new JedisPubSub() {

                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        Log.debug("Subscribed to channel: " + channel);
                    }

                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        Log.debug("Unsubscribed from channel: " + channel);
                    }

                    public void onMessage(String channel, String event) {
                        Log.debug("Received message from channel: " + channel);
                        Gson gson = new Gson();
                        try {
                            SeederSuccessful message = gson.fromJson(event, SeederSuccessful.class);
                            if (ENTITY_TYPE.equals(message.getHeaders().getEntityType())
                                    && EVENT_TYPE.equals(message.getHeaders().getEventType())) {
                                messageMediator.processMessage(message);
                            } else {
                                Log.debug("Ignoring message: " + message.toString());
                            }
                        } catch (Exception e) {
                            Log.error("Error processing message: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                };

                this.listenerJedis.subscribe(pubSub, channel);
            } catch (Exception e) {
                Log.error("Error in Redis Event listener: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (this.listenerJedis != null) {
                    this.listenerJedis.close();
                }
            }
        });
        listenerThread.start();
    }

    @PreDestroy
    public void shutdown() {
        if (this.listenerJedis != null) {
            this.listenerJedis.close();
        }
        if (this.listenerThread != null && listenerThread.isAlive()) {
            this.listenerThread.interrupt();
        }
    }

    private void initializeData() {
        RedisDataBootstrap redisDataBootstrap = new RedisDataBootstrap(this.listenerJedis);
        redisDataBootstrap.init();
    }
}
