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
import com.ncop.modules.inquiries.enums.OrderQuantityUnit;
import com.ncop.modules.inquiries.repository.CustomerInquiryRepository;
import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.enums.ProductSourcing;
import com.ncop.modules.products.repository.ProductRepository;
import com.ncop.modules.qa.services.QaRfqService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private final QaRfqService qaRfqService;

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
        User currentUser = currentUserOrNull();
        if (currentUser != null) {
            inquiry.setRaisedByUserId(currentUser.getId());
            inquiry.setRaisedByUserName(fullName(currentUser));
        }

        User salesAssignee = null;
        if (request.getSalesAssigneeId() != null && !request.getSalesAssigneeId().isBlank()) {
            salesAssignee = userRepository.findById(request.getSalesAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales assignee not found"));
            if (salesAssignee.getUserStatus() != UserStatus.ACTIVE || !hasRole(salesAssignee, "SALES")) {
                throw new IllegalArgumentException("Selected sales assignee must be an active SALES user");
            }
        } else if (currentUser != null && hasRole(currentUser, "SALES")) {
            salesAssignee = currentUser;
        }
        if (salesAssignee != null) {
            inquiry.setSalesAssigneeId(salesAssignee.getId());
            inquiry.setSalesAssigneeName(fullName(salesAssignee));
        }

        
        boolean hasQa = request.getLines().stream().anyMatch(l -> l.getSourcing() == ProductSourcing.IN_HOUSE);
        boolean hasQc = request.getLines().stream().anyMatch(l -> l.getSourcing() == ProductSourcing.OUTSOURCED);

        if (hasQa && (request.getQaAssigneeId() == null || request.getQaAssigneeId().isBlank())) {
            throw new IllegalArgumentException("QA Reviewer is required for in-house products");
        }
        if (hasQc && (request.getQcAssigneeId() == null || request.getQcAssigneeId().isBlank())) {
            throw new IllegalArgumentException("QC Reviewer is required for outsourced products");
        }

        if (hasQa) {
            User qa = userRepository.findById(request.getQaAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("QA Assignee not found"));
            if (qa.getUserStatus() != UserStatus.ACTIVE || !hasRole(qa, "QA")) {
                throw new IllegalArgumentException("Selected QA Assignee is invalid");
            }
            inquiry.setQaAssigneeId(qa.getId());
            inquiry.setQaAssigneeName(fullName(qa));
        }

        if (hasQc) {
            User qc = userRepository.findById(request.getQcAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("QC Assignee not found"));
            if (qc.getUserStatus() != UserStatus.ACTIVE || !hasRole(qc, "QC")) {
                throw new IllegalArgumentException("Selected QC Assignee is invalid");
            }
            inquiry.setQcAssigneeId(qc.getId());
            inquiry.setQcAssigneeName(fullName(qc));
        }

        List<InquiryLine> lines = request.getLines().stream().map(this::toLine).toList();

        inquiry.setLines(lines);
        
        
        inquiry.setStatus(hasQa && hasQc ? InquiryStatus.SUBMITTED
                : hasQa ? InquiryStatus.SUBMITTED_TO_QA : InquiryStatus.SUBMITTED_TO_QC);
        CustomerInquiry saved = inquiryRepository.save(inquiry);
        qaRfqService.syncAssignedInquiry(saved, client);
        return saved;
    }

    public Page<CustomerInquiry> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inquiryRepository.findAll(pageable);
    }

    public CustomerInquiry update(String id, CustomerInquiryRequestDto request) {
        CustomerInquiry inquiry = get(id);
        User currentUser = currentUserOrNull();
        if (currentUser == null) {
            throw new IllegalArgumentException("Authentication is required to update an inquiry");
        }
        boolean isAdmin = hasRole(currentUser, "ADMIN") || hasRole(currentUser, "SUPER_ADMIN");
        boolean isOwner = currentUser.getId().equals(inquiry.getRaisedByUserId())
                || currentUser.getId().equals(inquiry.getSalesAssigneeId());
        if (!isAdmin && !isOwner) {
            throw new IllegalArgumentException("Only the inquiry owner or an administrator can edit this inquiry");
        }

        Client client = clientRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        PointOfContact contact = client.getPointOfContacts().stream()
                .filter(p -> request.getContactPersonId().equals(p.getId())
                        || request.getContactPersonId().equalsIgnoreCase(p.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Contact person does not belong to the selected customer"));
        inquiry.setCustomerId(client.getId());
        inquiry.setCustomerName(client.getCompanyName());
        inquiry.setContactPersonId(contact.getId() != null ? contact.getId() : request.getContactPersonId());
        inquiry.setContactPersonName(contact.getPersonName());
        inquiry.setContactEmail(contact.getEmail());
        inquiry.setInquirySource(request.getInquirySource());
        inquiry.setPriority(request.getPriority());
        inquiry.setTargetQuoteDate(request.getTargetQuoteDate());
        inquiry.setNotes(request.getNotes());

        if (request.getSalesAssigneeId() != null && !request.getSalesAssigneeId().isBlank()) {
            if (!isAdmin) {
                throw new IllegalArgumentException("Only an administrator can change the sales owner");
            }
            User salesAssignee = userRepository.findById(request.getSalesAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales assignee not found"));
            if (salesAssignee.getUserStatus() != UserStatus.ACTIVE || !hasRole(salesAssignee, "SALES")) {
                throw new IllegalArgumentException("Selected sales assignee must be an active SALES user");
            }
            inquiry.setSalesAssigneeId(salesAssignee.getId());
            inquiry.setSalesAssigneeName(fullName(salesAssignee));
        } else if (isAdmin) {
            inquiry.setSalesAssigneeId(null);
            inquiry.setSalesAssigneeName(null);
        }

        boolean hasQa = request.getLines().stream().anyMatch(l -> l.getSourcing() == ProductSourcing.IN_HOUSE);
        boolean hasQc = request.getLines().stream().anyMatch(l -> l.getSourcing() == ProductSourcing.OUTSOURCED);
        if (hasQa && (request.getQaAssigneeId() == null || request.getQaAssigneeId().isBlank())) {
            throw new IllegalArgumentException("QA Reviewer is required for in-house products");
        }
        if (hasQc && (request.getQcAssigneeId() == null || request.getQcAssigneeId().isBlank())) {
            throw new IllegalArgumentException("QC Reviewer is required for outsourced products");
        }
        inquiry.setQaAssigneeId(null);
        inquiry.setQaAssigneeName(null);
        inquiry.setQcAssigneeId(null);
        inquiry.setQcAssigneeName(null);
        if (hasQa) {
            User qa = userRepository.findById(request.getQaAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("QA Assignee not found"));
            if (qa.getUserStatus() != UserStatus.ACTIVE || !hasRole(qa, "QA")) {
                throw new IllegalArgumentException("Selected QA Assignee is invalid");
            }
            inquiry.setQaAssigneeId(qa.getId());
            inquiry.setQaAssigneeName(fullName(qa));
        }
        if (hasQc) {
            User qc = userRepository.findById(request.getQcAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("QC Assignee not found"));
            if (qc.getUserStatus() != UserStatus.ACTIVE || !hasRole(qc, "QC")) {
                throw new IllegalArgumentException("Selected QC Assignee is invalid");
            }
            inquiry.setQcAssigneeId(qc.getId());
            inquiry.setQcAssigneeName(fullName(qc));
        }
        inquiry.setLines(request.getLines().stream().map(this::toLine).toList());
        inquiry.setStatus(hasQa && hasQc ? InquiryStatus.SUBMITTED
                : hasQa ? InquiryStatus.SUBMITTED_TO_QA : InquiryStatus.SUBMITTED_TO_QC);
        CustomerInquiry saved = inquiryRepository.save(inquiry);
        qaRfqService.syncAssignedInquiry(saved, client);
        return saved;
    }

    public CustomerInquiry get(String id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
    }

    public Page<CustomerInquiry> listAssignedToCurrentUser(int page, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            throw new IllegalArgumentException("Authentication is required to view assigned inquiries");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return inquiryRepository.findByQaAssigneeIdOrQcAssigneeIdOrRaisedByUserIdOrSalesAssigneeId(
                user.getId(), user.getId(), user.getId(), user.getId(), PageRequest.of(page, size));
    }

    private InquiryLine toLine(InquiryLineRequestDto request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
        ProductSourcing sourcing = request.getSourcing();
        

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
        
        
        line.setQuantityRequired(request.getQuantityRequired());
        line.setOrderQuantityUnit(request.getOrderQuantityUnit());
        line.setCalculatedTabletQuantity(calculateTabletQuantity(request));
        line.setTertiaryPackRequired(request.getTertiaryPackRequired());
        line.setSecondaryPackRequired(request.getSecondaryPackRequired());
        line.setMonoBoxPackRequired(request.getMonoBoxPackRequired());
        line.setStripPackRequired(request.getStripPackRequired());
        line.setTabletPackRequired(request.getTabletPackRequired());
        line.setTargetPrice(request.getTargetPrice());
        line.setPackagingNotes(request.getPackagingNotes());
        return line;
    }

    private long calculateTabletQuantity(InquiryLineRequestDto request) {
        long quantity = request.getQuantityRequired() == null ? 0 : request.getQuantityRequired();
        if (quantity < 1) throw new IllegalArgumentException("Quantity required must be greater than zero");
        OrderQuantityUnit unit = request.getOrderQuantityUnit();
        // Existing RFQs created before pack-level selection remain valid and use their requested quantity.
        if (unit == null || unit == OrderQuantityUnit.TABLET) return quantity;
        if (unit == OrderQuantityUnit.JAR) return multiply(quantity, request.getTabletPackRequired(), "tablets per jar");

        long result = quantity;
        if (unit == OrderQuantityUnit.TERTIARY)
            result = multiply(result, request.getTertiaryPackRequired(), "tertiary pack");
        if (unit == OrderQuantityUnit.TERTIARY || unit == OrderQuantityUnit.SECONDARY)
            result = multiply(result, request.getSecondaryPackRequired(), "secondary pack");
        if (unit != OrderQuantityUnit.STRIP)
            result = multiply(result, request.getMonoBoxPackRequired(), "mono box pack");
        return multiply(result, request.getStripPackRequired(), "strip pack");
    }

    private long multiply(long value, Long factor, String field) {
        if (factor == null || factor < 1) throw new IllegalArgumentException(field + " is required for the selected order level");
        try {
            return Math.multiplyExact(value, factor);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Calculated tablet quantity is too large");
        }
    }

    private boolean hasRole(User user, String roleName) {
        if (user.getRoleIds() == null || user.getRoleIds().isEmpty()) return false;
        return roleRepository.findAllById(user.getRoleIds()).stream()
                .anyMatch(role -> role.isActive() && roleName.equalsIgnoreCase(role.getName()));
    }

    private User currentUserOrNull() {
        String email = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        return email == null || email.isBlank() || "anonymousUser".equals(email)
                ? null : userRepository.findByEmail(email).orElse(null);
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
