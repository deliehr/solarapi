package de.liehrit.solarapi.components;

import com.google.gson.JsonSyntaxException;
import de.liehrit.solarapi.SolarapiApplication;
import de.liehrit.solarapi.model.LogMessage;
import de.liehrit.solarapi.model.WattMessage;
import de.liehrit.solarapi.repositories.LogRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.java.Log;
import lombok.val;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class MqttListener implements MqttCallback {
    private final String TOPIC;
    final private InfluxClient influxClient;
    private MqttAsyncClient client;
    private final Logger logger = LoggerFactory.getLogger(MqttListener.class);

    private final LogRepository logRepository;

    public MqttListener(Environment environment, InfluxClient influxClient, LogRepository logRepository) throws Exception {
        this.influxClient = influxClient;
        this.logRepository = logRepository;

        String mqttHost = environment.getRequiredProperty("MQTT.HOST");
        String mqttUsername = environment.getRequiredProperty("MQTT.USERNAME");
        String mqttPassword = environment.getRequiredProperty("MQTT.PASSWORD");
        String mqttClientId = environment.getRequiredProperty("MQTT.CLIENTID");
        TOPIC = environment.getRequiredProperty("MQTT.TOPIC");

        MqttConnectionOptions connectionOptions = new MqttConnectionOptions();

        connectionOptions.setServerURIs(new String[] {mqttHost});
        connectionOptions.setUserName(mqttUsername);
        connectionOptions.setPassword(mqttPassword.getBytes());
        connectionOptions.setAutomaticReconnect(true);

        MemoryPersistence persistence = new MemoryPersistence();

        try {
            UUID uuid = UUID.randomUUID();
            client = new MqttAsyncClient(mqttHost, mqttClientId, persistence);
            client.setCallback(this);

            IMqttToken connectToken = client.connect(connectionOptions);
            connectToken.waitForCompletion(6000);

            logger.info("connected to mqtt");

            IMqttToken subToken = client.subscribe(TOPIC, 0);
            subToken.waitForCompletion(6000);

            logger.debug("subscribed to mqtt topic: '{}'", TOPIC);
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    public void disconnectAndExit() {
        try {
            IMqttToken unsubscribeToken = client.unsubscribe(TOPIC);
            unsubscribeToken.waitForCompletion(6000);

            logger.info("unsubscribed from mqtt topic '{}'", TOPIC);

            IMqttToken disconnectToken = client.disconnect();
            disconnectToken.waitForCompletion(6000);

            logger.info("disconnected from mqtt");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    @PreDestroy
    private void onPreDestroy() {
        disconnectAndExit();
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

    }

    @Override
    public void mqttErrorOccurred(MqttException e) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        val messageContent = new String(message.getPayload());

        if(message.isRetained()) return;

        try {
            val timestamp = org.joda.time.DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ", Locale.GERMANY);
            val logMessage = LogMessage.builder().topic(topic).value(messageContent).timestamp(timestamp).build();

            logRepository.insert(logMessage);
        } catch (JsonSyntaxException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {

    }

    @Override
    public void connectComplete(boolean b, String s) {

    }

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {

    }
}

