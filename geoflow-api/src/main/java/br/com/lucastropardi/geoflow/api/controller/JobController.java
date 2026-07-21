package br.com.lucastropardi.geoflow.api.controller;

import br.com.lucastropardi.geoflow.api.dto.CreateJobRequest;
import br.com.lucastropardi.geoflow.api.dto.CreateJobResponse;
import br.com.lucastropardi.geoflow.api.dto.JobLogResponse;
import br.com.lucastropardi.geoflow.api.dto.JobResponse;
import br.com.lucastropardi.geoflow.api.service.JobService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateJobResponse createJob(@Valid @RequestBody CreateJobRequest request) {
        return jobService.createJob(request);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable Long id) {
        return jobService.getJob(id);
    }

    @GetMapping("/{id}/logs")
    public List<JobLogResponse> getJobLogs(@PathVariable Long id) {
        return jobService.getJobLogs(id);
    }
}
