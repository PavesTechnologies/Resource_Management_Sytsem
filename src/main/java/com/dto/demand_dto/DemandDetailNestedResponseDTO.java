package com.dto.demand_dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DemandDetailNestedResponseDTO {
    
    // Main Demand Information
    private UUID demandId;
    private String demandName;
    private String demandStatus;
    private String demandPriority;
    private String demandType;
    private String deliveryModel;
    private LocalDate demandStartDate;
    private LocalDate demandEndDate;
    private Double minExp;
    private Integer resourceRequired;
    private Integer allocation;
    private String demandJustification;
    private Integer priorityScore;
    
    // Nested Objects
    private RejectionInfo rejectionInfo;
    private ClientInfo clientInfo;
    private ProjectInfo projectInfo;
    private DemandskillsRequirements DemandskillsRequirements;
    private SLAInfo slaInfo;
    
    // Nested DTO Classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class RejectionInfo {
        private String rejectionReason;
        private String rmRejectionReason;
        private String dmRejectionReason;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class ClientInfo {
        private String clientName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class ProjectInfo {
        private Long projectId;
        private String projectName;
        private String deliveryModel;
        private String location;
        private String lifecycle;
        private String riskLevel;
        private String staffingReadiness;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class DemandskillsRequirements {
        private List<SkillDTO> requiredSkills;
        private List<CertificateDTO> requiredCertificates;
        private DeliveryRoleDetailDTO deliveryRoleDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class SkillDTO {
        private String skillName;
        private String subSkillName;
        private String proficiencyLevelName;
        private Boolean mandatoryFlag;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class CertificateDTO {
        private String certificateName;
        private String issuingAuthority;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class DeliveryRoleDetailDTO {
        private String roleName;
        private List<RoleSkillDTO> roleSkills;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class RoleSkillDTO {
        private String skillId;
        private String skillName;
        private String skillCategory;
        private String subSkillId;
        private String subSkillName;
        private String proficiencyLevelId;
        private String proficiencyLevelName;
        private Boolean mandatoryFlag;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class SLAInfo {
        private UUID demandSlaId;
        private String slaType;
        private Integer slaDurationDays;
        private Integer warningThresholdDays;
        private LocalDate slaCreatedAt;
        private LocalDate slaDueAt;
        private Boolean slaBreached;
        private Long remainingDays;
        private Long overdueDays;
        private LocalDate fulfillDate;
    }
}
