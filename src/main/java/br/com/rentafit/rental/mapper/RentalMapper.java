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
                .contractType(dto.contractType() != null ? dto.contractType() : 0)
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
        contract.setContractType(dto.contractType() != null ? dto.contractType() : contract.getContractType());
        contract.setCreatedByEmployeeId(dto.createdByEmployeeId() != null ? dto.createdByEmployeeId() : contract.getCreatedByEmployeeId());
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

        // Replace payments
        contract.getPayments().clear();
        if (dto.payments() != null) {
            dto.payments().stream()
                    .map(paymentDto -> toPaymentEntity(paymentDto, contract))
                    .forEach(contract.getPayments()::add);
        }
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
                .customerId(contract.getCustomerId())
                .customerName(contract.getCustomerName())
                .customerDocument(contract.getCustomerDocument())
                .createdByEmployeeId(contract.getCreatedByEmployeeId())
                .returnedByEmployeeId(contract.getReturnedByEmployeeId())
                .parentContractId(contract.getParentContractId())
                .replacedByContractId(contract.getReplacedByContractId())
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

    public RentalContractSummaryDTO toSummaryDTO(RentalContract contract) {
        BigDecimal totalValue = computeTotalValue(contract.getItems());
        BigDecimal paidValue  = computePaidValue(contract.getPayments());

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

