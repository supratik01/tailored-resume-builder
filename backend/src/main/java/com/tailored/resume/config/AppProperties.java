package com.tailored.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private OpenAi openai = new OpenAi();
    private Quota quota = new Quota();
    private Razorpay razorpay = new Razorpay();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTtlMinutes = 15;
        private long refreshTtlDays = 14;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5186");
    }

    /**
     * Free-tier limits. Tune {@code freeRunsPerMonth} once real API costs are known — it is a
     * config value precisely so it does not require a code change.
     */
    @Data
    public static class Quota {
        /** Tailoring runs a free user may start per calendar month. */
        private int freeRunsPerMonth = 3;
        /** Zone whose calendar month defines the reset boundary. */
        private String zone = "Asia/Kolkata";
    }

    /** Razorpay subscription billing. Test-mode keys are the same shape as live ones. */
    @Data
    public static class Razorpay {
        private String keyId;
        private String keySecret;
        /** Shared secret configured on the webhook in the Razorpay dashboard. */
        private String webhookSecret;
        /** The ₹499/month plan created in the dashboard. */
        private String planId;
        private String baseUrl = "https://api.razorpay.com/v1";
        private int timeoutSeconds = 20;

        /** True when enough is configured to talk to Razorpay at all. */
        public boolean isConfigured() {
            return notBlank(keyId) && notBlank(keySecret) && notBlank(planId);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    @Data
    public static class OpenAi {
        private String apiKey;
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1";
        private int timeoutSeconds = 60;
    }
}
