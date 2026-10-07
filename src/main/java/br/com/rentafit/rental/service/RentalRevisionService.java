package br.com.rentafit.rental.service;

import br.com.rentafit.auth.service.CurrentAccountId;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.*;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.RentalContractDetailsDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.PaidPaymentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalRevisionService {
    private final RentalContractRepository repository;
    private final RentalMapper mapper;
    private final CurrentAccountId currentAccountId;
    private final RentalReservationDelta reservationDelta;

    public RentalContractDetailsDTO create(UUID id, Supplier<String> legacyId) {
        RentalContract original = requireOriginal(id);
        requireEligible(original);
        // Se já existe uma revisão ativa para este contrato, retorna ela
        var active = repository.findByParentContractIdAndStatusNot(id, ContractStatus.SUPERSEDED);
        if (active.isPresent()) return mapper.toDetailsDTO(active.get(), null);
        RentalContract revision = copyContract(original, legacyId.get());
        RentalContract saved = repository.saveAndFlush(revision);
        original.setReplacedByContractId(saved.getId());
        repository.save(original);
        return mapper.toDetailsDTO(saved, null);
    }

    public List<UUID> pendingAccessoryIds(RentalContract revision, List<UUID> incoming) {
        if (revision.getStatus() != ContractStatus.REVISION) return incoming;
        RentalContract parent = repository.findById(revision.getParentContractId())
                .orElseThrow(() -> ResourceNotFoundException.forId("RentalContract", revision.getParentContractId()));
        if (parent.getStatus() != ContractStatus.FINALIZED) return incoming;
        java.util.Map<UUID, Long> reserved = new java.util.HashMap<>(reservationDelta.accessoryCounts(parent));
        return incoming.stream().filter(id -> {
            long available = reserved.getOrDefault(id, 0L);
            if (available == 0) return true;
            reserved.put(id, available - 1);
            return false;
        }).toList();
    }

    public RentalContractDetailsDTO restart(UUID id) {
        RentalContract revision = repository.findById(id).orElseThrow(() -> ResourceNotFoundException.forId("RentalContract", id));
        if (revision.getStatus() != ContractStatus.REVISION || revision.getParentContractId() == null) {
            throw new ValidationException("Contrato " + id + ": esperado REVISION para reiniciar alteração");
        }
        RentalContract original = requireOriginal(revision.getParentContractId());
        repository.lockById(id);
        requireEligible(original);
        revision.getItems().clear();
        revision.getItems().addAll(copyItems(original, revision));
        revision.getPayments().clear();
        repository.flush();
        revision.getPayments().addAll(copyPayments(original, revision));
        revision.setPickupDate(original.getPickupDate());
        revision.setEventDate(original.getEventDate());
        revision.setReturnDate(original.getReturnDate());
        revision.setNotes(original.getNotes());
        revision.setParentSnapshot(snapshot(original));
        return mapper.toDetailsDTO(repository.saveAndFlush(revision), null);
    }

    public void confirm(RentalContract revision) {
        RentalContract original = requireOriginal(revision.getParentContractId());
        requireEligible(original);
        if (!Objects.equals(revision.getParentSnapshot(), snapshot(original))) {
            throw new ValidationException("Revisão " + revision.getId() + " desatualizada; reinicie a alteração do contrato " + original.getId());
        }
        if (!PaidPaymentSnapshot.paid(original.getPayments()).equals(PaidPaymentSnapshot.paid(revision.getPayments()))) {
            throw new ValidationException("Valor pago da revisão " + revision.getId() + " deve preservar os pagamentos do original");
        }
        if (original.getStatus() == ContractStatus.FINALIZED) reservationDelta.apply(original, revision);
        revision.setStatus(original.getStatus());
        revision.setConfirmedByAccountId(currentAccountId.requireId());
        revision.setRevisionConfirmedAt(OffsetDateTime.now());
        original.setStatus(ContractStatus.SUPERSEDED);
        original.setReplacedByContractId(revision.getId());
        repository.save(original);
    }

    public static void requireEligible(RentalContract contract) {
        boolean eligibleStatus = contract.getStatus() == ContractStatus.SIGNED || contract.getStatus() == ContractStatus.FINALIZED;
        boolean physicalMovement = Boolean.TRUE.equals(contract.getReturned()) || contract.getItems().stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getDelivered()) || Boolean.TRUE.equals(item.getReturned())
                        || item.getMetadata().stream().anyMatch(meta -> Boolean.TRUE.equals(meta.getReturned())));
        if (!eligibleStatus || physicalMovement) {
            throw new ValidationException("Contrato " + contract.getId() + " em " + contract.getStatus()
                    + ": esperado SIGNED ou FINALIZED sem entrega/devolução para alterar");
        }
    }

    private RentalContract requireOriginal(UUID id) {
        repository.lockById(id);
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.forId("RentalContract", id));
    }

    private RentalContract copyContract(RentalContract original, String legacyId) {
        RentalContract revision = RentalContract.builder().legacyId(legacyId).contractType(original.getContractType())
                .customerId(original.getCustomerId()).customerName(original.getCustomerName()).customerDocument(original.getCustomerDocument())
                .createdByEmployeeId(original.getCreatedByEmployeeId()).pickupDate(original.getPickupDate())
                .eventDate(original.getEventDate()).returnDate(original.getReturnDate()).notes(original.getNotes())
                .printTemplateId(original.getPrintTemplateId()).status(ContractStatus.REVISION).parentContractId(original.getId())
                .revisedByAccountId(currentAccountId.requireId()).parentSnapshot(snapshot(original)).build();
        revision.setItems(copyItems(original, revision));
        revision.setPayments(copyPayments(original, revision));
        return revision;
    }

    private List<RentalContractItem> copyItems(RentalContract original, RentalContract revision) {
        // Deep-copy items + metadata
        return new ArrayList<>(original.getItems().stream().map(item -> {
            RentalContractItem copied = RentalContractItem.builder().contract(revision).rentalItemId(item.getRentalItemId())
                    .legacyProductCode(item.getLegacyProductCode()).description(item.getDescription()).value(item.getValue())
                    .attendantEmployeeId(item.getAttendantEmployeeId()).delivered(false).build();
            copied.setMetadata(new ArrayList<>(item.getMetadata().stream().map(meta -> RentalContractItemMeta.builder()
                    .contractItem(copied).type(meta.getType()).description(meta.getDescription()).accessoryId(meta.getAccessoryId()).build()).toList()));
            return copied;
        }).toList());
    }

    private List<RentalPayment> copyPayments(RentalContract original, RentalContract revision) {
        // Deep-copy payments (preserving status — PAID payments stay PAID)
        return new ArrayList<>(original.getPayments().stream().map(payment -> RentalPayment.builder().contract(revision)
                .installmentNumber(payment.getInstallmentNumber()).paymentDate(payment.getPaymentDate()).paymentMethod(payment.getPaymentMethod())
                .value(payment.getValue()).installments(payment.getInstallments()).processedByEmployeeId(payment.getProcessedByEmployeeId())
                .status(payment.getStatus()).build()).toList());
    }

    private String snapshot(RentalContract original) {
        var payments = original.getPayments().stream().map(payment -> payment.getId() + ":" + payment.getStatus()
                + ":" + PaidPaymentSnapshot.from(payment)).sorted().toList();
        var items = original.getItems().stream().map(item -> java.util.Arrays.asList(item.getId(), item.getRentalItemId(),
                item.getValue(), item.getAttendantEmployeeId(), item.getDelivered(), item.getReturned(), item.getMetadata().stream()
                        .map(meta -> java.util.Arrays.asList(meta.getId(), meta.getType(), meta.getDescription(), meta.getAccessoryId(), meta.getReturned()).toString())
                        .sorted().toList()).toString()).sorted().toList();
        return java.util.Arrays.asList(original.getStatus(), original.getCustomerId(), original.getCustomerName(), original.getCustomerDocument(),
                original.getPickupDate(), original.getEventDate(), original.getReturnDate(), original.getNotes(), original.getReturned(), payments, items).toString();
    }
}
