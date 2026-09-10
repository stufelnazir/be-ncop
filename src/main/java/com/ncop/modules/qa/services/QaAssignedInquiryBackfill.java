package com.ncop.modules.qa.services;

import com.ncop.modules.clients.repository.ClientRepository;
import com.ncop.modules.inquiries.repository.CustomerInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ensures Sales RFQs assigned before this handoff was introduced also reach QA. */
@Component
@RequiredArgsConstructor
@Slf4j
public class QaAssignedInquiryBackfill implements ApplicationRunner {

    private final CustomerInquiryRepository inquiryRepository;
    private final ClientRepository clientRepository;
    private final QaRfqService qaRfqService;

    @Override
    public void run(ApplicationArguments args) {
        inquiryRepository.findAll().stream()
                .forEach(inquiry -> {
                    try {
                        clientRepository.findById(inquiry.getCustomerId())
                                .ifPresentOrElse(
                                        client -> qaRfqService.syncAssignedInquiry(inquiry, client),
                                        () -> log.warn("Unable to backfill QA RFQ {}: customer {} was not found",
                                                inquiry.getRfqNo(), inquiry.getCustomerId()));
                    } catch (RuntimeException exception) {
                        log.error("Unable to backfill QA RFQ {}", inquiry.getRfqNo(), exception);
                    }
                });
    }
}
