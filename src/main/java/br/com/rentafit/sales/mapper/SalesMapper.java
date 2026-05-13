package br.com.rentafit.sales.mapper;

import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesOrderItem;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.domain.enums.SalesItemStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.*;
import br.com.rentafit.sales.port.RetailProductPort.RetailProductSnapshot;
import br.com.rentafit.sales.port.SalesCustomerPort.CustomerSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper entre entidades de domínio e DTOs do componente Sales.
 */
@Component
public class SalesMapper {

    // ── Order ────────────────────────────────────────────────────────────────

    public SalesOrder toEntity(CreateSalesOrderDTO dto, CustomerSnapshot customer) {
        SalesOrder order = SalesOrder.builder()
                .customerId(customer != null ? customer.id() : null)
                .customerName(customer != null ? customer.name() : null)
                .customerDocument(customer != null ? customer.document() : null)
                .createdByEmployeeId(dto.createdByEmployeeId())
                .notes(dto.notes() != null ? dto.notes() : "")
                .discountValue(dto.discountValue() != null ? dto.discountValue() : BigDecimal.ZERO)
                .status(SalesOrderStatus.DRAFT)
                .build();

        return order;
    }

    public void updateEntityFromDTO(SalesOrder order, UpdateSalesOrderDTO dto, CustomerSnapshot customer) {
        if (customer != null) {
            order.setCustomerId(customer.id());
            order.setCustomerName(customer.name());
            order.setCustomerDocument(customer.document());
        } else if (dto.customerId() == null) {
            order.setCustomerId(null);
            order.setCustomerName(null);
            order.setCustomerDocument(null);
        }
        order.setNotes(dto.notes() != null ? dto.notes() : "");
        order.setDiscountValue(dto.discountValue() != null ? dto.discountValue() : BigDecimal.ZERO);
    }

    public SalesOrderDetailsDTO toDetailsDTO(SalesOrder order, List<String> warnings) {
        BigDecimal subtotal = computeSubtotal(order.getItems());
        BigDecimal totalValue = subtotal.subtract(order.getDiscountValue()).max(BigDecimal.ZERO);
        BigDecimal paidValue = computePaidValue(order.getPayments());

        return SalesOrderDetailsDTO.builder()
                .id(order.getId())
                .legacyId(order.getLegacyId())
                .status(order.getStatus().name())
                .statusDescription(order.getStatus().getDescription())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerDocument(order.getCustomerDocument())
                .createdByEmployeeId(order.getCreatedByEmployeeId())
                .notes(order.getNotes())
                .discountValue(order.getDiscountValue())
                .invoiceStatus(order.getInvoiceStatus().name())
                .invoiceStatusDescription(order.getInvoiceStatus().getDescription())
                .invoiceId(order.getInvoiceId())
                .subtotal(subtotal)
                .totalValue(totalValue)
                .paidValue(paidValue)
                .remainingValue(totalValue.subtract(paidValue).max(BigDecimal.ZERO))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream().map(this::toItemDetailsDTO).collect(Collectors.toList()))
                .payments(order.getPayments().stream()
                        .sorted(Comparator.comparing(SalesPayment::getInstallmentNumber,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toPaymentDetailsDTO)
                        .collect(Collectors.toList()))
                .warnings(warnings)
                .build();
    }

    public SalesOrderSummaryDTO toSummaryDTO(SalesOrder order) {
        BigDecimal subtotal = computeSubtotal(order.getItems());
        BigDecimal totalValue = subtotal.subtract(order.getDiscountValue()).max(BigDecimal.ZERO);
        BigDecimal paidValue = computePaidValue(order.getPayments());

        return SalesOrderSummaryDTO.builder()
                .id(order.getId())
                .legacyId(order.getLegacyId())
                .status(order.getStatus().name())
                .statusDescription(order.getStatus().getDescription())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .invoiceStatus(order.getInvoiceStatus().name())
                .totalValue(totalValue)
                .paidValue(paidValue)
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build();
    }

    // ── Item ─────────────────────────────────────────────────────────────────

    public SalesOrderItem toItemEntity(SalesOrderItemInputDTO dto, SalesOrder order,
                                       RetailProductSnapshot product) {
        return SalesOrderItem.builder()
                .salesOrder(order)
                .retailProductId(product.id())
                .sku(product.sku())
                .description(product.name() + " — " + product.size() + " " + product.color())
                .unitPrice(product.value())
                .quantity(dto.quantity())
                .discountValue(dto.discountValue() != null ? dto.discountValue() : BigDecimal.ZERO)
                .itemStatus(SalesItemStatus.PENDING)
                .attendantEmployeeId(dto.attendantEmployeeId())
                .needsTailoring(dto.needsTailoring() != null ? dto.needsTailoring() : false)
                .tailoringNotes(dto.tailoringNotes())
                .build();
    }

    public SalesOrderItemDetailsDTO toItemDetailsDTO(SalesOrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .subtract(item.getDiscountValue());

        return SalesOrderItemDetailsDTO.builder()
                .id(item.getId())
                .retailProductId(item.getRetailProductId())
                .sku(item.getSku())
                .description(item.getDescription())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .discountValue(item.getDiscountValue())
                .totalValue(lineTotal.max(BigDecimal.ZERO))
                .itemStatus(item.getItemStatus().name())
                .itemStatusDescription(item.getItemStatus().getDescription())
                .attendantEmployeeId(item.getAttendantEmployeeId())
                .needsTailoring(item.getNeedsTailoring())
                .tailoringNotes(item.getTailoringNotes())
                .deliveredAt(item.getDeliveredAt())
                .deliveredByEmployeeId(item.getDeliveredByEmployeeId())
                .build();
    }

    // ── Payment ──────────────────────────────────────────────────────────────

    public SalesPayment toPaymentEntity(SalesPaymentInputDTO dto, SalesOrder order) {
        return SalesPayment.builder()
                .salesOrder(order)
                .installmentNumber(dto.installmentNumber())
                .paymentDate(dto.paymentDate())
                .paymentMethod(PaymentMethod.valueOf(dto.paymentMethod().toUpperCase()))
                .value(dto.value())
                .installments(dto.installments() != null ? dto.installments() : 1)
                .processedByEmployeeId(dto.processedByEmployeeId())
                .status(dto.status() != null ? PaymentStatus.valueOf(dto.status().toUpperCase()) : PaymentStatus.PENDING)
                .build();
    }

    public SalesPaymentDetailsDTO toPaymentDetailsDTO(SalesPayment payment) {
        return new SalesPaymentDetailsDTO(
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Subtotal = Σ(unitPrice × qty - itemDiscount) */
    public BigDecimal computeSubtotal(List<SalesOrderItem> items) {
        if (items == null) return BigDecimal.ZERO;
        return items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .subtract(item.getDiscountValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal computePaidValue(List<SalesPayment> payments) {
        if (payments == null) return BigDecimal.ZERO;
        return payments.stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .map(SalesPayment::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
