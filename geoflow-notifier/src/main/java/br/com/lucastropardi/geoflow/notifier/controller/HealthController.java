package br.com.lucastropardi.geoflow.notifier.controller;

import br.com.lucastropardi.geoflow.notifier.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("geoflow-notifier", "UP");
    }
}
