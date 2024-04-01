package de.liehrit.solarapi.components;

import com.google.gson.JsonSyntaxException;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import de.liehrit.solarapi.model.FieldConfiguration;
import de.liehrit.solarapi.model.LogMessage;
import de.liehrit.solarapi.repositories.LogRepository;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.TimeZone;
import java.util.UUID;

@Component(value="mycomponent")
public class MqttListener implements MqttCallback {
    private final String SUBSCRIPTION_TOPIC;
    private final String INVERTER;
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
        SUBSCRIPTION_TOPIC = environment.getRequiredProperty("MQTT.TOPIC");
        INVERTER = environment.getRequiredProperty("SOLAR.INVERTER");

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

            IMqttToken subToken = client.subscribe(SUBSCRIPTION_TOPIC, 0);
            subToken.waitForCompletion(6000);

            logger.debug("subscribed to mqtt topic: '{}'", SUBSCRIPTION_TOPIC);
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    public void disconnectAndExit() {
        try {
            IMqttToken unsubscribeToken = client.unsubscribe(SUBSCRIPTION_TOPIC);
            unsubscribeToken.waitForCompletion(6000);

            logger.info("unsubscribed from mqtt topic '{}'", SUBSCRIPTION_TOPIC);

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
    public void disconnected(MqttDisconnectResponse response) {
        logger.debug("mqtt disconnected. exception: {}, reason: {}, code: {}", response.getException().getLocalizedMessage(), response.getReasonString(), response.getReturnCode());
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
        logger.error(e.getLocalizedMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {

        logger.debug("message arrived: {}", message);

        val messageContent = new String(message.getPayload());

        logger.debug("messageContent arrived: {}", messageContent);


        val timeZone = TimeZone.getTimeZone("Europe/Berlin");
        val timestamp = (new LocalDateTime()).toDateTime(DateTimeZone.forTimeZone(timeZone)).getMillis(); // milliseconds

        if(message.isRetained()) {
            logger.debug("message is retained, do not proceed");
            return;
        }

        try {
            val logMessage = LogMessage.builder().topic(topic).value(messageContent).timestamp(timestamp).build();
            logRepository.insert(logMessage);
        } catch (JsonSyntaxException e) {
            logger.error(e.getLocalizedMessage());
        }

        // influx

        double fieldValue;

        try {
            fieldValue = Double.parseDouble(messageContent);
        } catch (NumberFormatException nfe) {
            logger.debug("cannot parse message content to double, value: {}", messageContent);
            return;
        }

        Point point = null;
        val topicParts = topic.split("/");

        if(topicParts.length == 2) {
            // general information
            val fieldName = topicParts[1];

            point = new Point("general")
                    .time(timestamp, WritePrecision.MS)
                    .addField(fieldName, fieldValue);
        } else if(topicParts.length == 3) {
            // total information
            val fieldName = topicParts[2];

            point = new Point("total")
                    .time(timestamp, WritePrecision.MS)
                    .addField(fieldName, fieldValue);
        } else if(topicParts.length == 4) {
            // channel specific information
            val channel = topicParts[2];
            val fieldName = topicParts[3];

            point = new Point("channels")
                    .time(timestamp, WritePrecision.MS)
                    .addField(fieldName, fieldValue)
                    .addTag("channel", channel);
        }

        if(point == null) {
            logger.debug("point is null");
            return;
        }

        try {
            influxClient.savePoint(point);
        } catch (InfluxException ie) {
            logger.error(ie.getLocalizedMessage());
            // log
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

