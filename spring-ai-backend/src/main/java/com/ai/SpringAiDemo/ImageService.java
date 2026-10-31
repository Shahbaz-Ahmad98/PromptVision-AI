
package com.ai.SpringAiDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImageService {

    private static final Logger log =
            LoggerFactory.getLogger(ImageService.class);

    private static final int HORDE_SIZE = 512;
    private static final int POLL_ATTEMPTS = 90;
    private static final long POLL_DELAY_MS = 2000;

    private final RestClient restClient;
    private final String provider;

    // NVIDIA FLUX.1-dev backup
    private final String nvidiaApiKey;
    private final String nvidiaImageUrl;

    // AI Horde active image provider
    private final String hordeBaseUrl;
    private final String hordeApiKey;
    private final String hordeClientAgent;

    public ImageService(
            RestClient.Builder builder,

            @Value("${app.image.provider:aihorde}")
            String provider,

            @Value("${nvidia.api.key}")
            String nvidiaApiKey,

            @Value("${nvidia.image.url}")
            String nvidiaImageUrl,

            @Value("${aihorde.base-url:https://aihorde.net/api/v2}")
            String hordeBaseUrl,

            @Value("${aihorde.api-key:0000000000}")
            String hordeApiKey,

            @Value("${aihorde.client-agent:SpringAiStudio:1.0:local-development}")
            String hordeClientAgent
    ) {

        this.restClient = builder.build();

        this.provider =
                provider.trim().toLowerCase(Locale.ROOT);

        this.nvidiaApiKey = nvidiaApiKey;
        this.nvidiaImageUrl = nvidiaImageUrl;

        this.hordeBaseUrl =
                hordeBaseUrl.replaceAll("/+$", "");

        this.hordeApiKey = hordeApiKey;
        this.hordeClientAgent = hordeClientAgent;

        if (!this.provider.equals("aihorde")
                && !this.provider.equals("nvidia")) {

            throw new IllegalArgumentException(
                    "Unsupported image provider: " + provider
            );
        }

        log.info("Active image provider: {}", this.provider);
    }

    // ==========================================
    // SINGLE IMAGE GENERATION
    // ==========================================

    public byte[] generateImage(
            String prompt,
            String quality,
            int width,
            int height,
            Integer steps,
            double cfgScale,
            int seed
    ) {

        String normalizedQuality = validateInput(
                prompt,
                quality,
                width,
                height,
                cfgScale,
                seed
        );

        if (provider.equals("aihorde")) {

            return generateHordeImages(
                    prompt,
                    1,
                    normalizedQuality,
                    steps,
                    cfgScale,
                    seed
            ).get(0);
        }

        return generateNvidiaImage(
                prompt,
                normalizedQuality,
                width,
                height,
                steps,
                cfgScale,
                seed
        );
    }

    // ==========================================
    // MULTIPLE IMAGE GENERATION
    // ==========================================

    public List<String> generateImages(
            String prompt,
            int n,
            String quality,
            int width,
            int height,
            Integer steps,
            double cfgScale,
            int seed
    ) {

        if (n < 1 || n > 4) {
            throw new IllegalArgumentException(
                    "Number of images must be between 1 and 4"
            );
        }

        String normalizedQuality = validateInput(
                prompt,
                quality,
                width,
                height,
                cfgScale,
                seed
        );

        List<byte[]> images;

        if (provider.equals("aihorde")) {

            // One batch request to AI Horde.
            images = generateHordeImages(
                    prompt,
                    n,
                    normalizedQuality,
                    steps,
                    cfgScale,
                    seed
            );

        } else {

            if (seed != 0
                    && (long) seed + n - 1 > Integer.MAX_VALUE) {

                throw new IllegalArgumentException(
                        "Seed exceeds integer range"
                );
            }

            images = new ArrayList<>();

            for (int i = 0; i < n; i++) {

                int currentSeed =
                        seed == 0 ? 0 : seed + i;

                byte[] imageBytes = generateNvidiaImage(
                        prompt,
                        normalizedQuality,
                        width,
                        height,
                        steps,
                        cfgScale,
                        currentSeed
                );

                images.add(imageBytes);
            }
        }

        List<String> dataUrls = new ArrayList<>();

        for (byte[] image : images) {

            String base64 = Base64.getEncoder()
                    .encodeToString(image);

            String dataUrl =
                    "data:"
                            + detectMimeType(image)
                            + ";base64,"
                            + base64;

            dataUrls.add(dataUrl);
        }

        return dataUrls;
    }

    // ==========================================
    // INPUT VALIDATION
    // ==========================================

    private String validateInput(
            String prompt,
            String quality,
            int width,
            int height,
            double cfgScale,
            int seed
    ) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Image prompt is required"
            );
        }

        if (quality == null) {
            throw new IllegalArgumentException(
                    "Quality is required"
            );
        }

        String normalizedQuality =
                quality.trim().toLowerCase(Locale.ROOT);

        if (!normalizedQuality.equals("standard")
                && !normalizedQuality.equals("high")) {

            throw new IllegalArgumentException(
                    "Quality must be standard or high"
            );
        }

        // Preserve the existing React request contract.
        // AI Horde generates 512x512 images internally.
        if (width != 1024 || height != 1024) {

            throw new IllegalArgumentException(
                    "This application currently requires 1024x1024 requests"
            );
        }

        if (seed < 0) {
            throw new IllegalArgumentException(
                    "Seed must not be negative"
            );
        }

        if (!Double.isFinite(cfgScale)
                || cfgScale < 0
                || cfgScale > 9) {

            throw new IllegalArgumentException(
                    "CFG scale must be between 0 and 9"
            );
        }

        return normalizedQuality;
    }

    // ==========================================
    // AI HORDE IMAGE GENERATION
    // ==========================================

    private List<byte[]> generateHordeImages(
            String prompt,
            int count,
            String quality,
            Integer steps,
            double cfgScale,
            int seed
    ) {

        if (prompt.length() > 5000) {
            throw new IllegalArgumentException(
                    "AI Horde prompt is too long"
            );
        }

        // Updated AI Horde presets:
        // Standard: 20 steps
        // High: 25 steps
        int finalSteps = steps == null
                ? (quality.equals("high") ? 25 : 20)
                : steps;

        if (finalSteps < 5 || finalSteps > 50) {
            throw new IllegalArgumentException(
                    "AI Horde steps must be between 5 and 50"
            );
        }

        Map<String, Object> params = new HashMap<>();

        params.put("n", count);
        params.put("width", HORDE_SIZE);
        params.put("height", HORDE_SIZE);
        params.put("steps", finalSteps);
        params.put("cfg_scale", cfgScale);
        params.put("sampler_name", "k_euler_a");

        if (seed != 0) {
            params.put("seed", String.valueOf(seed));
        }

        Map<String, Object> request = new HashMap<>();

        request.put("prompt", prompt);
        request.put("params", params);
        request.put("nsfw", false);
        request.put("censor_nsfw", true);
        request.put("trusted_workers", false);
        request.put("slow_workers", true);
        request.put("shared", false);
        request.put("r2", true);

        String requestId = null;
        boolean completed = false;

        try {

            // STEP 1: SUBMIT REQUEST

            log.info(
                    "Submitting AI Horde request: count={}, steps={}, size={}x{}",
                    count,
                    finalSteps,
                    HORDE_SIZE,
                    HORDE_SIZE
            );

            Map<?, ?> submission = restClient.post()
                    .uri(hordeBaseUrl + "/generate/async")
                    .header("apikey", hordeApiKey)
                    .header("Client-Agent", hordeClientAgent)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (submission == null
                    || submission.get("id") == null) {

                throw new IllegalStateException(
                        "AI Horde did not return a request ID"
                );
            }

            requestId = submission.get("id").toString();

            log.info(
                    "AI Horde request submitted successfully"
            );

            // STEP 2: CHECK QUEUE STATUS

            for (int attempt = 0;
                 attempt < POLL_ATTEMPTS;
                 attempt++) {

                Map<?, ?> check = restClient.get()
                        .uri(
                                hordeBaseUrl
                                        + "/generate/check/{id}",
                                requestId
                        )
                        .header("apikey", hordeApiKey)
                        .header("Client-Agent", hordeClientAgent)
                        .retrieve()
                        .body(Map.class);

                if (check != null) {

                    if (Boolean.TRUE.equals(
                            check.get("faulted"))) {

                        throw new IllegalStateException(
                                "AI Horde generation failed"
                        );
                    }

                    if (Boolean.TRUE.equals(
                            check.get("done"))) {

                        completed = true;
                        break;
                    }

                    log.info(
                            "AI Horde queue: waiting={}, processing={}, finished={}",
                            check.get("waiting"),
                            check.get("processing"),
                            check.get("finished")
                    );
                }

                try {
                    Thread.sleep(POLL_DELAY_MS);
                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "AI Horde request interrupted",
                            ex
                    );
                }
            }

            if (!completed) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI Horde queue timed out. Try again later."
                );
            }

            // STEP 3: RETRIEVE RESULTS

            Map<?, ?> status = restClient.get()
                    .uri(
                            hordeBaseUrl
                                    + "/generate/status/{id}",
                            requestId
                    )
                    .header("apikey", hordeApiKey)
                    .header("Client-Agent", hordeClientAgent)
                    .retrieve()
                    .body(Map.class);

            if (status == null
                    || !(status.get("generations")
                    instanceof List<?> generations)
                    || generations.isEmpty()) {

                throw new IllegalStateException(
                        "AI Horde returned no generated images"
                );
            }

            // STEP 4: DOWNLOAD IMAGE DATA

            List<byte[]> images = new ArrayList<>();

            for (Object item : generations) {

                if (!(item instanceof Map<?, ?> generation)) {
                    continue;
                }

                Object raw = generation.get("img");

                if (!(raw instanceof String location)
                        || location.isBlank()) {
                    continue;
                }

                byte[] imageBytes;

                if (location.startsWith("data:")) {

                    imageBytes = decodeImage(location);

                } else if (location.startsWith("https://")) {

                    URI uri = URI.create(location);

                    if (uri.getHost() == null
                            || uri.getUserInfo() != null) {

                        throw new IllegalStateException(
                                "AI Horde returned an invalid image URL"
                        );
                    }

                    imageBytes = restClient.get()
                            .uri(uri)
                            .retrieve()
                            .body(byte[].class);

                } else {

                    imageBytes = decodeImage(location);
                }

                if (imageBytes != null
                        && imageBytes.length > 0) {

                    detectMimeType(imageBytes);
                    images.add(imageBytes);
                }
            }

            if (images.isEmpty()) {
                throw new IllegalStateException(
                        "AI Horde returned no usable images"
                );
            }

            log.info(
                    "AI Horde generated {} image(s)",
                    images.size()
            );

            return images;

        } catch (RestClientResponseException ex) {

            throw handleProviderError("AI Horde", ex);

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cannot connect to AI Horde",
                    ex
            );

        } finally {

            // Cancel unfinished requests.

            if (requestId != null && !completed) {
                try {

                    restClient.delete()
                            .uri(
                                    hordeBaseUrl
                                            + "/generate/status/{id}",
                                    requestId
                            )
                            .header("apikey", hordeApiKey)
                            .header("Client-Agent", hordeClientAgent)
                            .retrieve()
                            .toBodilessEntity();

                } catch (Exception ex) {

                    log.warn(
                            "Could not cancel unfinished AI Horde request"
                    );
                }
            }
        }
    }

    // ==========================================
    // NVIDIA FLUX.1-DEV BACKUP
    // ==========================================

    private byte[] generateNvidiaImage(
            String prompt,
            String quality,
            int width,
            int height,
            Integer steps,
            double cfgScale,
            int seed
    ) {

        if (prompt.length() > 10000) {
            throw new IllegalArgumentException(
                    "NVIDIA prompt must be 10000 characters or less"
            );
        }

        // NVIDIA settings stay unchanged.
        int finalSteps = steps == null
                ? (quality.equals("high") ? 50 : 30)
                : steps;

        if (finalSteps < 5 || finalSteps > 100) {
            throw new IllegalArgumentException(
                    "NVIDIA steps must be between 5 and 100"
            );
        }

        Map<String, Object> body = Map.of(
                "prompt", prompt,
                "mode", "base",
                "width", width,
                "height", height,
                "cfg_scale", cfgScale,
                "steps", finalSteps,
                "samples", 1,
                "seed", seed
        );

        log.info(
                "Generating NVIDIA image: steps={}, size={}x{}",
                finalSteps,
                width,
                height
        );

        try {

            Map<?, ?> response = restClient.post()
                    .uri(nvidiaImageUrl)
                    .headers(headers ->
                            headers.setBearerAuth(nvidiaApiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null
                    || !(response.get("artifacts")
                    instanceof List<?> artifacts)
                    || artifacts.isEmpty()
                    || !(artifacts.get(0)
                    instanceof Map<?, ?> first)) {

                throw new IllegalStateException(
                        "NVIDIA returned no image artifacts"
                );
            }

            Object encoded = first.get("base64");

            if (!(encoded instanceof String base64)
                    || base64.isBlank()) {

                throw new IllegalStateException(
                        "NVIDIA returned no Base64 image"
                );
            }

            byte[] imageBytes = decodeImage(base64);

            detectMimeType(imageBytes);

            log.info(
                    "NVIDIA image generated successfully: {} bytes",
                    imageBytes.length
            );

            return imageBytes;

        } catch (RestClientResponseException ex) {

            throw handleProviderError("NVIDIA", ex);

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cannot connect to NVIDIA image API",
                    ex
            );
        }
    }

    // ==========================================
    // BASE64 DECODING
    // ==========================================

    private byte[] decodeImage(String encoded) {

        if (encoded.startsWith("data:")) {

            int commaIndex = encoded.indexOf(',');

            if (commaIndex < 0) {
                throw new IllegalStateException(
                        "Invalid image data URI"
                );
            }

            encoded = encoded.substring(commaIndex + 1);
        }

        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Image provider returned invalid Base64",
                    ex
            );
        }
    }

    // ==========================================
    // IMAGE MIME TYPE
    // ==========================================

    public String detectMimeType(byte[] image) {

        // JPEG
        if (image.length >= 3
                && (image[0] & 0xff) == 0xff
                && (image[1] & 0xff) == 0xd8
                && (image[2] & 0xff) == 0xff) {

            return "image/jpeg";
        }

        // PNG
        if (image.length >= 8
                && (image[0] & 0xff) == 0x89
                && (image[1] & 0xff) == 0x50
                && (image[2] & 0xff) == 0x4e
                && (image[3] & 0xff) == 0x47) {

            return "image/png";
        }

        // WEBP
        if (image.length >= 12
                && image[0] == 'R'
                && image[1] == 'I'
                && image[2] == 'F'
                && image[3] == 'F'
                && image[8] == 'W'
                && image[9] == 'E'
                && image[10] == 'B'
                && image[11] == 'P') {

            return "image/webp";
        }

        throw new IllegalStateException(
                "Unsupported image format returned by provider"
        );
    }

    // ==========================================
    // PROVIDER ERROR HANDLING
    // ==========================================

    private ResponseStatusException handleProviderError(
            String providerName,
            RestClientResponseException ex
    ) {

        int status = ex.getStatusCode().value();

        log.error(
                "{} image API returned HTTP {}",
                providerName,
                status
        );

        if (status == 429) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    providerName + " rate limit reached",
                    ex
            );
        }

        if (status == 401 || status == 403) {
            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    providerName + " API authentication or access failed",
                    ex
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                providerName + " image API returned HTTP " + status,
                ex
        );
    }
}
