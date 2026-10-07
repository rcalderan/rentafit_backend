package br.com.rentafit.rental.service;

import br.com.rentafit.rental.domain.*;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.RentalContractDetailsDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.RentalContractValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalProposalDuplication {
    private final RentalContractRepository repository;
    private final RentalContractValidator validator;
    private final RentalMapper mapper;

    public RentalContractDetailsDTO create(RentalContract original, Supplier<String> legacyId) {
        // Atualiza snapshot do cliente
        var customer = validator.validateAndGetCustomer(original.getCustomerId());
        RentalContract copy = RentalContract.builder().legacyId(legacyId.get()).contractType(original.getContractType())
                .customerId(customer.id()).customerName(customer.name()).customerDocument(customer.document())
                .createdByEmployeeId(original.getCreatedByEmployeeId()).pickupDate(original.getPickupDate())
                .eventDate(original.getEventDate()).returnDate(original.getReturnDate())
                .notes("Duplicado do contrato " + original.getId() + ". " + original.getNotes())
                .status(ContractStatus.DRAFT).returned(false).build();
        copy.setItems(new ArrayList<>(original.getItems().stream().map(item -> copyItem(item, copy)).toList()));
        RentalContract saved = repository.save(copy);
        log.info("Contract {} duplicated as {} (legacyId={})", original.getId(), saved.getId(), saved.getLegacyId());
        return mapper.toDetailsDTO(saved, null);
    }

    private RentalContractItem copyItem(RentalContractItem item, RentalContract copy) {
        RentalContractItem copied = RentalContractItem.builder().contract(copy).rentalItemId(item.getRentalItemId())
                .legacyProductCode(item.getLegacyProductCode()).description(item.getDescription()).value(item.getValue())
                .attendantEmployeeId(item.getAttendantEmployeeId()).delivered(false).build();
        copied.setMetadata(new ArrayList<>(item.getMetadata().stream().map(meta -> RentalContractItemMeta.builder()
                .contractItem(copied).type(meta.getType()).description(meta.getDescription()).accessoryId(meta.getAccessoryId())
                .build()).toList()));
        return copied;
    }
}
