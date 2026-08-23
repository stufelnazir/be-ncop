package com.ncop.modules.inquiries.services;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.exception.ResourceNotFoundException;
import com.ncop.auth.model.Role;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.repository.UserRepository;
import com.ncop.modules.clients.entity.Client;
import com.ncop.modules.clients.entity.PointOfContact;
import com.ncop.modules.clients.repository.ClientRepository;
import com.ncop.modules.inquiries.dto.CustomerInquiryRequestDto;
import com.ncop.modules.inquiries.dto.InquiryLineRequestDto;
import com.ncop.modules.inquiries.entity.CustomerInquiry;
import com.ncop.modules.inquiries.entity.InquiryLine;
import com.ncop.modules.inquiries.enums.InquiryStatus;
import com.ncop.modules.inquiries.repository.CustomerInquiryRepository;
import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.enums.ProductSourcing;
import com.ncop.modules.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerInquiryService {
    private final CustomerInquiryRepository inquiryRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomerInquiry create(CustomerInquiryRequestDto request) {
        Client client = clientRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        PointOfContact contact = client.getPointOfContacts().stream()
                .filter(p -> request.getContactPersonId().equals(p.getId())
                        || request.getContactPersonId().equalsIgnoreCase(p.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Contact person does not belong to the selected customer"));

        CustomerInquiry inquiry = new CustomerInquiry();
        inquiry.setRfqNo(nextRfqNo());
        inquiry.setInquiryDate(LocalDate.now());
        inquiry.setCustomerId(client.getId());
        inquiry.setCustomerName(client.getCompanyName());
        inquiry.setContactPersonId(contact.getId() != null ? contact.getId() : request.getContactPersonId());
        inquiry.setContactPersonName(contact.getPersonName());
        inquiry.setContactEmail(contact.getEmail());
        inquiry.setInquirySource(request.getInquirySource());
        inquiry.setPriority(request.getPriority());
        inquiry.setTargetQuoteDate(request.getTargetQuoteDate());
        inquiry.setNotes(request.getNotes());

        List<InquiryLine> lines = request.getLines().stream().map(this::toLine).toList();
        inquiry.setLines(lines);
        boolean hasQa = lines.stream().anyMatch(l -> l.getSourcing() == ProductSourcing.IN_HOUSE);
        boolean hasQc = lines.stream().anyMatch(l -> l.getSourcing() == ProductSourcing.OUTSOURCED);
        inquiry.setStatus(hasQa && hasQc ? InquiryStatus.SUBMITTED
                : hasQa ? InquiryStatus.SUBMITTED_TO_QA : InquiryStatus.SUBMITTED_TO_QC);
        return inquiryRepository.save(inquiry);
    }

    public Page<CustomerInquiry> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inquiryRepository.findAll(pageable);
    }

    public CustomerInquiry get(String id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
    }

    private InquiryLine toLine(InquiryLineRequestDto request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
        ProductSourcing sourcing = request.getSourcing();
        User assignee = userRepository.findById(request.getQualityAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Quality assignee not found"));
        String requiredRole = sourcing == ProductSourcing.OUTSOURCED ? "QC" : "QA";
        if (assignee.getUserStatus() != UserStatus.ACTIVE || !hasRole(assignee, requiredRole)) {
            throw new IllegalArgumentException("Selected assignee must be an active " + requiredRole + " user for this product");
        }

        InquiryLine line = new InquiryLine();
        line.setProductId(product.getId());
        line.setProductName(product.getBrandName());
        line.setGenericName(product.getIngredients() == null ? "" : product.getIngredients().stream()
                .map(i -> i.getApi()).filter(v -> v != null && !v.isBlank()).reduce((a, b) -> a + " + " + b).orElse(""));
        line.setDosageForm(product.getDosageForm());
        line.setDosageVariant(product.getDosageVariant());
        line.setStrength(product.getIngredients() == null ? "" : product.getIngredients().stream()
                .map(i -> (i.getStrength() == null ? "" : i.getStrength()) + (i.getUnit() == null ? "" : i.getUnit()))
                .reduce((a, b) -> a + " + " + b).orElse(""));
        line.setPharmacopeia(product.getIngredients() == null ? "" : product.getIngredients().stream()
                .map(i -> i.getPharmacopeia()).filter(v -> v != null && !v.isBlank()).distinct().reduce((a, b) -> a + "/" + b).orElse(""));
        line.setSourcing(sourcing);
        line.setQualityAssigneeId(assignee.getId());
        line.setQualityAssigneeName(fullName(assignee));
        line.setQuantityRequired(request.getQuantityRequired());
        line.setShipperPackRequired(request.getShipperPackRequired());
        line.setTertiaryPackRequired(request.getTertiaryPackRequired());
        line.setSecondaryPackRequired(request.getSecondaryPackRequired());
        line.setMonoBoxPackRequired(request.getMonoBoxPackRequired());
        line.setStripPackRequired(request.getStripPackRequired());
        line.setTabletPackRequired(request.getTabletPackRequired());
        line.setTargetPrice(request.getTargetPrice());
        line.setPackagingNotes(request.getPackagingNotes());
        return line;
    }

    private boolean hasRole(User user, String roleName) {
        if (user.getRoleIds() == null || user.getRoleIds().isEmpty()) return false;
        return roleRepository.findAllById(user.getRoleIds()).stream()
                .anyMatch(role -> role.isActive() && roleName.equalsIgnoreCase(role.getName()));
    }

    private String fullName(User user) {
        String value = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return value.isBlank() ? user.getEmail() : value;
    }

    private String nextRfqNo() {
        String prefix = "RFQ-" + Year.now().getValue() + "-";
        long sequence = inquiryRepository.count() + 1;
        String candidate = prefix + String.format("%05d", sequence);
        while (inquiryRepository.existsByRfqNo(candidate)) {
            candidate = prefix + String.format("%05d", ++sequence);
        }
        return candidate;
    }
}
