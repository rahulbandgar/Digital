package com.digital.ethaccumulator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "accumulator")
@Data
public class AppConfig {

    private String walletAddress;
    private String network = "sepolia";
    private boolean dryRun = true;
    private String claimCron = "0 0 8 * * ?"; // 08:00 UTC daily

    private List<FaucetConfig> faucets;

    @Data
    public static class FaucetConfig {
        private String name;
        private String type; // pow | api
        private String url;
        private String apiKey;
        private boolean enabled = true;
    }
}
