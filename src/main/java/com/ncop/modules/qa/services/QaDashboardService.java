package com.ncop.modules.qa.services;

import com.ncop.modules.qa.dto.QaKpiDto;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.repository.QaMfrRepository;
import com.ncop.modules.qa.repository.QaQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QaDashboardService {

    private final QaRfqService rfqService;
    private final QaMfrRepository mfrRepository;
    private final QaQueryRepository queryRepository;

    public QaKpiDto getKpis() {
        QaKpiDto kpi = new QaKpiDto();
        var visibleRfqs = rfqService.getVisibleRfqs();

        long formulaPending = visibleRfqs.stream().filter(r -> r.getStatus() == QaRfqStatus.FORMULA_PENDING).count();
        long specPending = visibleRfqs.stream().filter(r -> r.getStatus() == QaRfqStatus.SPECIFICATION_PENDING).count();
        long draftSaved = visibleRfqs.stream().filter(r -> r.getStatus() == QaRfqStatus.DRAFT_SAVED).count();
        long completed = visibleRfqs.stream().filter(r -> r.getStatus() == QaRfqStatus.COMPLETED).count();
        long queryRaised = visibleRfqs.stream().filter(r -> r.getStatus() == QaRfqStatus.QUERY_RAISED).count();
        long totalRfqs = visibleRfqs.size();

        kpi.setPendingFormulaCount(formulaPending);
        kpi.setSpecPendingCount(specPending);
        kpi.setDraftSavedCount(draftSaved);
        kpi.setCompletedCount(completed);
        kpi.setQueryRaisedCount(queryRaised);
        kpi.setTotalRfqs(totalRfqs);

        kpi.setOpenQueriesCount(queryRepository.countByStatus("OPEN"));
        kpi.setApprovedMfrCount(mfrRepository.countByStatus(QaMfrStatus.APPROVED));
        kpi.setTotalMfrs(mfrRepository.count());

        // Spec alignment: My Pending Tasks = Formula Pending + Spec Pending
        kpi.setMyPendingTasksCount(formulaPending + specPending);

        // Spec alignment: Overdue Tasks = RFQs with due date < today and not completed
        String todayStr = java.time.LocalDate.now().toString();
        long overdue = visibleRfqs.stream()
                .filter(r -> r.getStatus() != QaRfqStatus.COMPLETED)
                .filter(r -> r.getDueDate() != null && !r.getDueDate().isBlank() && r.getDueDate().compareTo(todayStr) < 0)
                .count();
        kpi.setOverdueTasksCount(overdue);

        // Spec alignment: Completed Today = RFQs updated/completed today
        java.time.Instant startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        long completedToday = visibleRfqs.stream()
                .filter(r -> r.getStatus() == QaRfqStatus.COMPLETED)
                .filter(r -> r.getLastUpdatedOn() != null && r.getLastUpdatedOn().isAfter(startOfToday))
                .count();
        kpi.setCompletedTodayCount(completedToday);

        return kpi;
    }
}
