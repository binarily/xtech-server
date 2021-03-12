package dk.comiu.server;


import dk.comiu.server.db.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Slf4j
public class LoadDatabase {

    protected static final boolean DEV_SWITCH = true;

    @Bean
    @Autowired
    public CommandLineRunner initDatabase(
            ColourRepository colourRepository,
            DeviceRepository deviceRepository,
            SegmentRepository segmentRepository,
            SkuRepository skuRepository,
            OrderRepository orderRepository
    ) {
        if (!DEV_SWITCH) {
            return args -> log.info("Database init ignored (no dev switch).");
        }
        //Clear all
        orderRepository.deleteAll();
        skuRepository.deleteAll();
        colourRepository.deleteAll();
        segmentRepository.deleteAll();
        deviceRepository.deleteAll();

        return args -> {
            log.info("Preloading " + deviceRepository.save(new Device(1, "Server", Device.DeviceType.SERVER, "http://127.0.0.1:8080")));
            Device counter = deviceRepository.save(new Device(2, "Counter", Device.DeviceType.COUNTER, "http://127.0.0.1:8081"));
            log.info("Preloading " + counter);
            log.info("Preloading " + deviceRepository.save(new Device(3, "Table", Device.DeviceType.TABLE, "http://127.0.0.1:8082")));

            log.info("Preloading " + colourRepository.save(new Colour(1, counter, "Red", null)));
            log.info("Preloading " + colourRepository.save(new Colour(2, counter, "Green", null)));
            log.info("Preloading " + colourRepository.save(new Colour(3, counter, "Blue", null)));

            log.info("Preloading " + segmentRepository.save(new Segment(1, counter, false)));
            log.info("Preloading " + segmentRepository.save(new Segment(2, counter, false)));
            log.info("Preloading " + segmentRepository.save(new Segment(3, counter, false)));
            log.info("Preloading " + segmentRepository.save(new Segment(4, counter, false)));

            log.info("Preloading " + skuRepository.save(new Sku(1L, "Carlsberg", BigDecimal.valueOf(12.0))));
            log.info("Preloading " + skuRepository.save(new Sku(2L, "Tuborg", BigDecimal.valueOf(12.0))));
            log.info("Preloading " + skuRepository.save(new Sku(3L, "Okocim", BigDecimal.valueOf(15.0))));
            log.info("Preloading " + skuRepository.save(new Sku(4L, "Desperados", BigDecimal.valueOf(15.0))));
            log.info("Preloading " + skuRepository.save(new Sku(5L, "Sommersby", BigDecimal.valueOf(10.0))));
            log.info("Preloading " + skuRepository.save(new Sku(6L, "Kronenburg", BigDecimal.valueOf(15.0))));
            log.info("Preloading " + skuRepository.save(new Sku(7L, "Kustosz", BigDecimal.valueOf(20.0))));
            log.info("Preloading " + skuRepository.save(new Sku(8L, "Grolsch", BigDecimal.valueOf(12.0))));

        };
    }
}