package com.ncop.modules.clients.services;

import java.util.List;
import java.util.Optional;

import com.ncop.common.services.EmailService;
import org.springframework.stereotype.Service;

import com.ncop.modules.clients.dto.ClientRequestDto;
import com.ncop.modules.clients.entity.Client;
import com.ncop.modules.clients.entity.PointOfContact;
import com.ncop.modules.clients.repository.ClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final EmailService emailService;

    public Client createClient(ClientRequestDto requestDto) {
        Client client = new Client();
        
        // Auto-generate customer code
        long count = clientRepository.count();
        String customerCode = String.format("CUST-%06d", count + 1);
        while (clientRepository.existsByCustomerCode(customerCode)) {
            count++;
            customerCode = String.format("CUST-%06d", count + 1);
        }
        client.setCustomerCode(customerCode);
        client.setCustomerType(requestDto.getCustomerType());
        client.setCompanyName(requestDto.getCompanyName());
        client.setTradeName(requestDto.getTradeName());
        client.setAnnualTurnover(requestDto.getAnnualTurnover());
        client.setPaymentTerms(requestDto.getPaymentTerms());
        client.setAddresses(requestDto.getAddresses());
        client.setPointOfContacts(requestDto.getPointOfContacts());

        Client savedClient = clientRepository.save(client);
        
        // Trigger emails
        if (savedClient.getPointOfContacts() != null) {
            for (PointOfContact poc : savedClient.getPointOfContacts()) {
                if (poc.getEmail() != null && !poc.getEmail().isEmpty()) {
                    // Assuming frontend runs on localhost:3000 for local dev
                    String registrationLink = "http://localhost:3000/register-client/" + savedClient.getId();
                    emailService.sendRegistrationEmail(poc.getEmail(), savedClient.getCompanyName(), registrationLink);
                }
            }
        }
        
        return savedClient;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public com.ncop.common.dto.PageResponse<Client> getClients(org.springframework.data.domain.Pageable pageable, String search) {
        List<Client> all = clientRepository.findAll();
        List<Client> filtered = all.stream().filter(c -> {
            if (search == null || search.isBlank()) return true;
            String q = search.toLowerCase();
            return (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(q)) ||
                    (c.getCustomerCode() != null && c.getCustomerCode().toLowerCase().contains(q)) ||
                    (c.getTradeName() != null && c.getTradeName().toLowerCase().contains(q)) ||
                    (c.getAddresses() != null && c.getAddresses().stream().anyMatch(a -> a.getCountry() != null && a.getCountry().toLowerCase().contains(q)));
        }).toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Client> paged = filtered.subList(fromIndex, toIndex);

        return com.ncop.common.dto.PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    public Optional<Client> getClientById(String id) {
        return clientRepository.findById(id);
    }

    public Client updateClient(String id, ClientRequestDto requestDto) {
        return clientRepository.findById(id).map(client -> {
            client.setCustomerType(requestDto.getCustomerType());
            client.setCompanyName(requestDto.getCompanyName());
            client.setTradeName(requestDto.getTradeName());
            client.setAnnualTurnover(requestDto.getAnnualTurnover());
            client.setPaymentTerms(requestDto.getPaymentTerms());
            client.setAddresses(requestDto.getAddresses());
            client.setPointOfContacts(requestDto.getPointOfContacts());
            return clientRepository.save(client);
        }).orElseThrow(() -> new RuntimeException("Client not found with id " + id));
    }
}
