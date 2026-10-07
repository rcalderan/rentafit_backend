package br.com.rentafit.rental.mapper;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.port.CustomerPort.CustomerSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper entre entidades de domínio e DTOs do componente Rental.
 */
@Component
public class RentalMapper {

    // ── Contract ──────────────────────────────────────────────────────────────

    public RentalContract toEntity(CreateRentalContractDTO dto, CustomerSnapshot snapshot) {
        RentalContract contract = RentalContract.builder()
                .legacyId(dto.legacyId())
                .contractType(0)
                .customerId(snapshot.id())
                .customerName(snapshot.name())
                .customerDocument(snapshot.document())
                .createdByEmployeeId(dto.createdByEmployeeId())
                .pickupDate(dto.pickupDate())
                .eventDate(dto.eventDate())
                .returnDate(dto.returnDate())
                .notes(dto.notes() != null ? dto.notes() : "")
                .status(ContractStatus.DRAFT)
                .returned(false)
                .build();

        if (dto.items() != null) {
            List<RentalContractItem> items = dto.items().stream()
                    .map(itemDto -> toItemEntity(itemDto, contract))
                    .collect(Collectors.toList());
            contract.setItems(items);
        }

        if (dto.payments() != null) {
            List<RentalPayment> payments = dto.payments().stream()
                    .map(paymentDto -> toPaymentEntity(paymentDto, contract))
                    .collect(Collectors.toList());
            contract.setPayments(payments);
        }

        return contract;
    }

    public void updateEntityFromDTO(RentalContract contract, UpdateRentalContractDTO dto) {
        updateEntityFromDTO(contract, dto, dto.payments());
    }

    public void updateEntityFromDTO(RentalContract contract, UpdateRentalContractDTO dto,
                                     List<RentalPaymentInputDTO> payments) {
        // contractType não é alterável — definido na criação
        if (contract.getCreatedByEmployeeId() == null) contract.setCreatedByEmployeeId(dto.createdByEmployeeId());
        contract.setPickupDate(dto.pickupDate());
        contract.setEventDate(dto.eventDate());
        contract.setReturnDate(dto.returnDate());
        contract.setNotes(dto.notes() != null ? dto.notes() : "");

        // Replace items
        contract.getItems().clear();
        if (dto.items() != null) {
            dto.items().stream()
                    .map(itemDto -> toItemEntity(itemDto, contract))
                    .forEach(contract.getItems()::add);
        }

        // Replace payments (using the provided list, which may include auto-generated entries)
        reconcilePayments(contract, payments == null ? List.of() : payments);
    }

    private void reconcilePayments(RentalContract contract, List<RentalPaymentInputDTO> incoming) {
        List<RentalPayment> existing = List.copyOf(contract.getPayments());
        java.util.Set<RentalPayment> retained = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (RentalPaymentInputDTO dto : incoming) {
            RentalPayment matched = existing.stream().filter(payment -> !retained.contains(payment))
                    .filter(payment -> payment.getInstallmentNumber().equals(dto.installmentNumber()))
                    .min(java.util.Comparator.comparing(payment -> payment.getStatus().name().equalsIgnoreCase(dto.status()) ? 0 : 1))
                    .orElseGet(() -> toPaymentEntity(dto, contract));
            if (contract.getStatus() != ContractStatus.REVISION || matched.getStatus() != PaymentStatus.PAID) applyPayment(matched, dto);
            if (matched.getId() == null && !contract.getPayments().contains(matched)) contract.getPayments().add(matched);
            retained.add(matched);
        }
        contract.getPayments().removeIf(payment -> !retained.contains(payment));
    }

    private void applyPayment(RentalPayment payment, RentalPaymentInputDTO dto) {
        payment.setInstallmentNumber(dto.installmentNumber());
        payment.setPaymentDate(dto.paymentDate());
        payment.setPaymentMethod(PaymentMethod.valueOf(dto.paymentMethod().toUpperCase(java.util.Locale.ROOT)));
        payment.setValue(dto.value());
        payment.setInstallments(dto.installments() == null ? 1 : dto.installments());
        payment.setProcessedByEmployeeId(dto.processedByEmployeeId());
        payment.setStatus(dto.status() == null ? PaymentStatus.PENDING : PaymentStatus.valueOf(dto.status().toUpperCase(java.util.Locale.ROOT)));
    }

    public RentalContractDetailsDTO toDetailsDTO(RentalContract contract, List<String> warnings) {
        return toDetailsDTO(contract, contract.getPayments(), warnings);
    }

    public RentalContractDetailsDTO toDetailsDTO(RentalContract contract, List<RentalPayment> payments, List<String> warnings) {
        BigDecimal totalValue = computeTotalValue(contract.getItems());
        BigDecimal paidValue  = computePaidValue(contract.getPayments());

        return RentalContractDetailsDTO.builder()
                .id(contract.getId())
                .legacyId(contract.getLegacyId())
                .contractType(contract.getContractType())
                .status(contract.getStatus().getLegacyCode())
                .statusDescription(contract.getStatus().getDescription())
                .printTemplateId(contract.getPrintTemplateId())
                .customerId(contract.getCustomerId())
                .customerName(contract.getCustomerName())
                .customerDocument(contract.getCustomerDocument())
                .createdByEmployeeId(contract.getCreatedByEmployeeId())
                .returnedByEmployeeId(contract.getReturnedByEmployeeId())
                .parentContractId(contract.getParentContractId())
                .replacedByContractId(contract.getReplacedByContractId())
                .revisedByAccountId(contract.getRevisedByAccountId())
                .confirmedByAccountId(contract.getConfirmedByAccountId())
                .revisionConfirmedAt(contract.getRevisionConfirmedAt())
                .pickupDate(contract.getPickupDate())
                .eventDate(contract.getEventDate())
                .returnDate(contract.getReturnDate())
                .actualReturnDate(contract.getActualReturnDate())
                .returned(contract.getReturned())
                .notes(contract.getNotes())
                .createdAt(contract.getCreatedAt())
                .totalValue(totalValue)
                .paidValue(paidValue)
                .remainingValue(totalValue.subtract(paidValue).max(BigDecimal.ZERO))
                .items(contract.getItems().stream().map(this::toItemDetailsDTO).collect(Collectors.toList()))
                .payments(payments.stream()
                        .sorted(Comparator.comparing(RentalPayment::getInstallmentNumber,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toPaymentDetailsDTO)
                        .collect(Collectors.toList()))
                .warnings(warnings)
                .build();
    }

    /**
     * Monta o DTO de listagem com totais pré-agregados via SQL.
     * Não acessa items/payments — evita disparar o SUBSELECT fetch.
     */
    public RentalContractSummaryDTO toSummaryDTO(RentalContract contract, BigDecimal totalValue, BigDecimal paidValue) {
        return RentalContractSummaryDTO.builder()
                .id(contract.getId())
                .legacyId(contract.getLegacyId())
                .contractType(contract.getContractType())
                .status(contract.getStatus().name())
                .statusDescription(contract.getStatus().getDescription())
                .customerId(contract.getCustomerId())
                .customerName(contract.getCustomerName())
                .parentContractId(contract.getParentContractId())
                .replacedByContractId(contract.getReplacedByContractId())
                .eventDate(contract.getEventDate())
                .pickupDate(contract.getPickupDate())
                .returnDate(contract.getReturnDate())
                .returned(contract.getReturned())
                .totalValue(totalValue)
                .paidValue(paidValue)
                .createdAt(contract.getCreatedAt())
                .build();
    }

    /**
     * DTO de reserva ativa por item (endpoint byItem). Não acessa items/payments —
     * apenas campos escalares do contrato + legacyId do cliente resolvido via CustomerPort.
     */
    public ItemReservationDTO toItemReservationDTO(RentalContract contract, Integer customerLegacyId) {
        return new ItemReservationDTO(
                contract.getId(),
                contract.getLegacyId(),
                contract.getCustomerId(),
                contract.getCustomerName(),
                customerLegacyId,
                contract.getEventDate(),
                contract.getPickupDate(),
                contract.getReturnDate(),
                contract.getStatus().name(),
                contract.getStatus().getDescription()
        );
    }

    // ── Item ──────────────────────────────────────────────────────────────────

    public RentalContractItem toItemEntity(ContractItemInputDTO dto, RentalContract contract) {
        RentalContractItem item = RentalContractItem.builder()
                .contract(contract)
                .rentalItemId(dto.rentalItemId())
                .legacyProductCode(dto.legacyProductCode())
                .description(dto.description())
                .value(dto.value())
                .attendantEmployeeId(dto.attendantEmployeeId())
                .delivered(false)
                .build();

        if (dto.metadata() != null) {
            List<RentalContractItemMeta> metaList = dto.metadata().stream()
                    .map(metaDto -> toMetaEntity(metaDto, item))
                    .collect(Collectors.toList());
            item.setMetadata(metaList);
        }

        return item;
    }

    public RentalContractItemDetailsDTO toItemDetailsDTO(RentalContractItem item) {
        List<RentalContractItemDetailsDTO.RentalContractItemMetaDTO> metaDtos =
                item.getMetadata() == null ? Collections.emptyList() :
                item.getMetadata().stream().map(this::toMetaDetailsDTO).collect(Collectors.toList());

        return new RentalContractItemDetailsDTO(
                item.getId(),
                item.getRentalItemId(),
                item.getLegacyProductCode(),
                item.getDescription(),
                item.getValue(),
                item.getDelivered(),
                item.getAttendantEmployeeId(),
                metaDtos
        );
    }

    // ── Meta ──────────────────────────────────────────────────────────────────

    public RentalContractItemMeta toMetaEntity(ContractItemMetaInputDTO dto, RentalContractItem item) {
        return RentalContractItemMeta.builder()
                .contractItem(item)
                .type(ItemMetaType.valueOf(dto.type().toUpperCase()))
                .description(dto.description())
                .accessoryId(dto.accessoryId())
                .build();
    }

    public RentalContractItemDetailsDTO.RentalContractItemMetaDTO toMetaDetailsDTO(RentalContractItemMeta meta) {
        return new RentalContractItemDetailsDTO.RentalContractItemMetaDTO(
                meta.getId(),
                meta.getType().name(),
                meta.getType().getDescription(),
                meta.getDescription(),
                meta.getAccessoryId()
        );
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    public RentalPayment toPaymentEntity(RentalPaymentInputDTO dto, RentalContract contract) {
        return RentalPayment.builder()
                .contract(contract)
                .installmentNumber(dto.installmentNumber())
                .paymentDate(dto.paymentDate())
                .paymentMethod(PaymentMethod.valueOf(dto.paymentMethod().toUpperCase()))
                .value(dto.value())
                .installments(dto.installments() != null ? dto.installments() : 1)
                .processedByEmployeeId(dto.processedByEmployeeId())
                .status(dto.status() != null ? PaymentStatus.valueOf(dto.status().toUpperCase()) : PaymentStatus.PENDING)
                .build();
    }

    public RentalPaymentDetailsDTO toPaymentDetailsDTO(RentalPayment payment) {
        return new RentalPaymentDetailsDTO(
                payment.getId(),
                payment.getInstallmentNumber(),
                payment.getPaymentDate(),
                payment.getPaymentMethod().name(),
                payment.getPaymentMethod().getLabel(),
                payment.getValue(),
                payment.getInstallments(),
                payment.getProcessedByEmployeeId(),
                payment.getStatus().name(),
                payment.getStatus().getDescription()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal computeTotalValue(List<RentalContractItem> items) {
        if (items == null) return BigDecimal.ZERO;
        return items.stream()
                .map(RentalContractItem::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computePaidValue(List<RentalPayment> payments) {
        if (payments == null) return BigDecimal.ZERO;
        return payments.stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .map(RentalPayment::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

