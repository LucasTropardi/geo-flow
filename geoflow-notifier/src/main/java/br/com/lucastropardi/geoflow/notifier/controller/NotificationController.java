package br.com.lucastropardi.geoflow.notifier.controller;

import br.com.lucastropardi.geoflow.notifier.service.NotifierSseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotifierSseService notifierSseService;

    public NotificationController(NotifierSseService notifierSseService) {
        this.notifierSseService = notifierSseService;
    }

    @GetMapping("/jobs/{jobId}/stream")
    public SseEmitter streamJobStatus(@PathVariable Long jobId) {
        return notifierSseService.subscribe(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }
}
