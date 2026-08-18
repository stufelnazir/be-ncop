package com.ncop.modules.clientmaster.services;

import java.util.List;
import java.util.Optional;

import com.ncop.common.services.EmailService;
import org.springframework.stereotype.Service;

import com.ncop.modules.clientmaster.dto.ClientRequestDto;
import com.ncop.modules.clientmaster.entity.Client;
import com.ncop.modules.clientmaster.entity.PointOfContact;
import com.ncop.modules.clientmaster.repository.ClientRepository;

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
        client.setCustomerCode(requestDto.getCustomerCode());
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
