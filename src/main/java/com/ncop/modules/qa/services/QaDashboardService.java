package com.ncop.modules.qa.services;

import com.ncop.modules.qa.dto.QaKpiDto;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.repository.QaMfrRepository;
import com.ncop.modules.qa.repository.QaQueryRepository;
import com.ncop.modules.qa.repository.QaRfqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QaDashboardService {

    private final QaRfqRepository rfqRepository;
    private final QaMfrRepository mfrRepository;
    private final QaQueryRepository queryRepository;

    public QaKpiDto getKpis() {
        QaKpiDto kpi = new QaKpiDto();

        long formulaPending = rfqRepository.countByStatus(QaRfqStatus.FORMULA_PENDING);
        long specPending = rfqRepository.countByStatus(QaRfqStatus.SPECIFICATION_PENDING);
        long draftSaved = rfqRepository.countByStatus(QaRfqStatus.DRAFT_SAVED);
        long completed = rfqRepository.countByStatus(QaRfqStatus.COMPLETED);
        long queryRaised = rfqRepository.countByStatus(QaRfqStatus.QUERY_RAISED);
        long totalRfqs = rfqRepository.count();

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
        long overdue = rfqRepository.findAll().stream()
                .filter(r -> r.getStatus() != QaRfqStatus.COMPLETED)
                .filter(r -> r.getDueDate() != null && !r.getDueDate().isBlank() && r.getDueDate().compareTo(todayStr) < 0)
                .count();
        kpi.setOverdueTasksCount(overdue);

        // Spec alignment: Completed Today = RFQs updated/completed today
        java.time.Instant startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        long completedToday = rfqRepository.findAll().stream()
                .filter(r -> r.getStatus() == QaRfqStatus.COMPLETED)
                .filter(r -> r.getLastUpdatedOn() != null && r.getLastUpdatedOn().isAfter(startOfToday))
                .count();
        // If 0, fallback to approved MFRs count if completed today
        if (completedToday == 0) {
            completedToday = mfrRepository.findAll().stream()
                    .filter(m -> m.getStatus() == QaMfrStatus.APPROVED)
                    .filter(m -> m.getLastUpdatedOn() != null && m.getLastUpdatedOn().isAfter(startOfToday))
                    .count();
        }
        kpi.setCompletedTodayCount(completedToday);

        return kpi;
    }
}
