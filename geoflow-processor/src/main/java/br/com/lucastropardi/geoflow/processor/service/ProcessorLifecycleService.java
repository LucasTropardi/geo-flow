package br.com.lucastropardi.geoflow.processor.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessorLifecycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorLifecycleService.class);

    @PostConstruct
    public void onStartup() {
        LOGGER.info("GeoFlow processor service initialized");
    }
}
