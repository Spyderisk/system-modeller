package uk.ac.soton.itinnovation.security.systemmodeller.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;

import java.time.LocalDateTime;

import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.AsyncService.RecStatus;

@Document(collection = "recommendations")
public class RecommendationEntity {

    @Id
    private String id;
    private RecommendationReportDTO report;
    private RecStatus status;
    private String message;
    private String modelId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    // getters and setters
    public void setStatus(RecStatus status) {
        this.status = status;
    }
    public RecStatus getStatus() {
        return this.status;
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setReport(RecommendationReportDTO report) {
        this.report = report;
    }
    public RecommendationReportDTO getReport() {
        return this.report;
    }
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
    public void setMesage(String msg) {
        this.message = msg;
    }
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    public String getModelId() {
        return this.modelId;
    }
}

