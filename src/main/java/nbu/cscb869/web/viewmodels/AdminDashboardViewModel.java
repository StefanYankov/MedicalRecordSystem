package nbu.cscb869.web.viewmodels;

import lombok.Data;
import nbu.cscb869.services.data.dtos.VisitViewDTO;

import java.util.List;

/**
 * View model for the admin dashboard, containing summary statistics.
 */
@Data
public class AdminDashboardViewModel {
    private long totalPatients;
    private long totalDoctors;
    private long totalVisits;
    private long totalDiagnoses;
    private long totalSickLeaves;
    private long unapprovedDoctorsCount;
    private List<VisitViewDTO> recentVisits;
}
