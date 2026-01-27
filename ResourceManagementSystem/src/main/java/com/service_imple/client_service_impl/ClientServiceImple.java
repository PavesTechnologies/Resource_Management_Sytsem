package com.service_imple.client_service_impl;

import com.dto.ApiResponse;
import com.dto.ClientDTO;
import com.dto.ClientFilterDTO;
import com.dto.PageResponse;
import com.entity.client_entities.Client;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientMapper;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import static org.springframework.util.StringUtils.hasText;


@Service
public class ClientServiceImple implements ClientService {

    @Autowired
    ClientRepo clientRepo;

    private final ClientMapper clientMapper;

    public ClientServiceImple(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    public class ClientSpecification {

        public static Specification<Client> build(ClientFilterDTO filter) {

            return (root, query, cb) -> {

                List<Predicate> predicates = new ArrayList<>();

                if (hasText(filter.getClientName())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("clientName")),
                            "%" + filter.getClientName().toLowerCase() + "%"
                    ));
                }

                if (hasText(filter.getClientType())) {
                    predicates.add(cb.equal(root.get("clientType"), filter.getClientType()));
                }

                if (hasText(filter.getPriorityLevel())) {
                    predicates.add(cb.equal(root.get("priorityLevel"), filter.getPriorityLevel()));
                }

                if (hasText(filter.getDeliveryModel())) {
                    predicates.add(cb.equal(root.get("deliveryModel"), filter.getDeliveryModel()));
                }

                if (hasText(filter.getRegionCode())) {
                    predicates.add(cb.equal(root.get("regionCode"), filter.getRegionCode()));
                }

                if (hasText(filter.getRegionName())) {
                    predicates.add(cb.equal(root.get("regionName"), filter.getRegionName()));
                }

                if (hasText(filter.getDefaultTimezone())) {
                    predicates.add(cb.equal(root.get("defaultTimezone"), filter.getDefaultTimezone()));
                }

                if (hasText(filter.getStatus())) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getCreatedFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedFrom()));
                }

                if (filter.getCreatedTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedTo()));
                }

                if (filter.getUpdatedFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("updatedAt"), filter.getUpdatedFrom()));
                }

                if (filter.getUpdatedTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("updatedAt"), filter.getUpdatedTo()));
                }

                // ✅ No predicates → return ALL records
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }


    @Override
    public ResponseEntity<ApiResponse> createClient(Client client) {
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        Client c = clientRepo.save(client);

        ApiResponse<Client> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(c!=null);
        apiResponse.setMessage(c != null
                ? "Client Created Successfully"
                : "Client Creation Failed");
        apiResponse.setData(c);

        return ResponseEntity.ok(apiResponse);

    }

    @Override
    public ApiResponse<PageResponse<ClientDTO>> searchClients(
            ClientFilterDTO filter,
            int page,
            int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            // Build specification from filter
            Specification<Client> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (hasText(filter.getClientName())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("clientName")),
                            "%" + filter.getClientName().toLowerCase() + "%"
                    ));
                }

                if (hasText(filter.getClientType())) {
                    predicates.add(cb.equal(root.get("clientType"), filter.getClientType()));
                }

                if (hasText(filter.getPriorityLevel())) {
                    predicates.add(cb.equal(root.get("priorityLevel"), filter.getPriorityLevel()));
                }

                if (hasText(filter.getDeliveryModel())) {
                    predicates.add(cb.equal(root.get("deliveryModel"), filter.getDeliveryModel()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Client> result = clientRepo.findAll(spec, pageable);

            // No records found
            if (result.isEmpty()) {
                ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setMessage("No clients found for the given filters");
                response.setData(null);
                return response;
            }

            // Records found
            PageResponse<ClientDTO> pageResponse = new PageResponse<>(
                    result.getContent()
                            .stream()
                            .map(clientMapper::toDto)
                            .toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages()
            );
            ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Records Fentched Successfully");
            response.setData(pageResponse);
            return response;
        } catch (Exception e) {
            ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Error fetching clients");
            response.setData(null);
            return response;
        }
    }
}

