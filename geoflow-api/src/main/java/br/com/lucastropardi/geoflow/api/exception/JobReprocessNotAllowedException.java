package br.com.lucastropardi.geoflow.api.exception;

public class JobReprocessNotAllowedException extends RuntimeException {

    public JobReprocessNotAllowedException(Long jobId, String currentStatus) {
        super("Processing job " + jobId + " cannot be reprocessed from status " + currentStatus);
    }
}
