package uk.ac.soton.itinnovation.security.systemmodeller.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.RecommendationsService.RecommendationJobState;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;

@Document(collection = "recommendations")
public class RecommendationEntity {

    @Id
    private String id;
    private RecommendationReportDTO report;
    private RecommendationJobState state;
    private String message;
    private String modelId;
    private int validRec = 0;
    private int totalReci = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    // getters and setters
    public void setState(RecommendationJobState state) {
        this.state = state;
    }
    public RecommendationJobState getState() {
        return this.state;
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

