package com.trello.config;

import com.trello.repository.UserRepository;
import com.trello.service.DataSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeederRunner {

    private final Environment environment;
    private final DataSeederService dataSeederService;
    private final UserRepository userRepository;

    /**
     * Auto-seed data on application startup if:
     * SEED_DATA_ON_STARTUP=true (explicitly enabled via Spring Profile)
     */
    @Bean
    public CommandLineRunner seedDataOnStartup() {
        return args -> {
            try {
                boolean shouldSeed = Boolean.parseBoolean(
                    environment.getProperty("seed.data.on-startup", "false")
                );

                // Only seed if explicitly enabled via configuration
                if (shouldSeed) {
                    log.info("🌱 Auto-seeding database with fake data...");
                    dataSeederService.seedAllData();
                    log.info("✅ Database seeding completed!");
                } else {
                    log.info("ℹ️  Data seeding disabled for this environment.");
                    log.info("   Manual seed: curl -X POST http://localhost:8088/api/admin/seed-data");
                }
            } catch (Exception e) {
                log.warn("⚠️  Could not seed data", e);
            }
        };
    }
}
