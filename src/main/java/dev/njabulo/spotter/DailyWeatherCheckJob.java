package dev.njabulo.spotter;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.njabulo.reporter.NotificationSender;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class DailyWeatherCheckJob {
    private static final Logger logger = LoggerFactory.getLogger(DailyWeatherCheckJob.class);
    private static final String ACCU_WEATHER_DAILY_FORECAST_BASE_URL = "http://dataservice.accuweather.com/forecasts/v1/daily/1day/";

    @ConfigProperty(name = "location.id")
    long locationId;
    @ConfigProperty(name = "api.key")
    String apiKey;

    @Inject
    NotificationSender reporter;

    @Scheduled(every = "20s", identity = "weather-forecast-job")
    public void executeForecastCheckTask() {
        // Weather lookup
        String url = makeForecastUrl(locationId, apiKey);
        Headline warning = checkForSevereForecast(url);
        if (warning != null) {
            reporter.auditHeadline(warning);
        }
    }

    private String makeForecastUrl(long locationId, String apiKey) {
        return ACCU_WEATHER_DAILY_FORECAST_BASE_URL + locationId +
                "?apikey=" +
                apiKey +
                "&details=true";
    }

    private Headline checkForSevereForecast(String url) {
        CloseableHttpResponse httpResponse;
        final String[] content = new String[1];
        Headline headline = null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGetOne = new HttpGet(url);
            httpResponse = (CloseableHttpResponse) httpClient.execute(httpGetOne, externalCallHttpResponse -> {
                content[0] = new String(externalCallHttpResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                return externalCallHttpResponse;
            });

            logger.info("HTTP Response Code: {}", httpResponse.getCode());
            if (httpResponse.getCode() == HttpStatus.SC_SUCCESS) {
                logger.info("Endpoint response received. Next, interpreting the result...");
                headline = extrapolateNotableHeadlineFromResult(content[0]);
            } else {
                logger.info("Endpoint response fault: {} , {}", httpResponse.getCode(), content[0]);
            }
        } catch (IOException exception) {
            logger.error("Third party call error", exception);
        }

        return headline;
    }

    private Headline extrapolateNotableHeadlineFromResult(String weatherContent) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Read JSON String into a JsonNode
            JsonNode weatherInfoInJsonFormat = objectMapper.readTree(weatherContent);
            int startEpochDate = weatherInfoInJsonFormat.get("Headline").get("EffectiveEpochDate").asInt();
            String startDateTime = weatherInfoInJsonFormat.get("Headline").get("EffectiveDate").asText();
            LocalDateTime effectiveDateTime = LocalDateTime.parse(startDateTime, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            int severity = weatherInfoInJsonFormat.get("Headline").get("Severity").asInt();
            String headlineText = weatherInfoInJsonFormat.get("Headline").get("Text").asText();
            String category = weatherInfoInJsonFormat.get("Headline").get("Category").asText();

            return new Headline(effectiveDateTime, startEpochDate, severity, headlineText, category);
        } catch (JsonProcessingException e) {
            logger.error("Failed to JSONify weather info", e);
        }
        return null;
    }
}
