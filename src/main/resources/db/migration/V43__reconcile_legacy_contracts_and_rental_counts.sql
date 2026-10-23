-- V43__reconcile_legacy_contracts_and_rental_counts.sql
-- Saneamento do histórico legado:
-- 1) Contratos FINALIZED já devolvidos (is_returned = true) passam a CLOSED,
--    alinhando o banco ao ciclo atual (FINALIZED → devolução → CLOSED) e
--    removendo-os das consultas de reserva pendente.
-- 2) rental_items.rental_count e last_rental_date reconciliados a partir do
--    histórico real de contratos (peça que saiu da loja ou contrato com baixa).

-- ── rental_contracts: FINALIZED + devolvido → CLOSED ─────────────────────────
UPDATE rental_contracts
SET status = 'CLOSED'
WHERE status = 'FINALIZED'
  AND is_returned = true;

-- ── rental_items: reconciliar contadores a partir do uso efetivo ─────────────
-- Critério espelha o runtime (RentalWorkflowService.recordRentalUsage): conta
-- contratos FINALIZED/CLOSED em que a peça foi entregue (is_delivered) ou o
-- contrato inteiro teve baixa (is_returned). GREATEST preserva o nloc legado
-- quando maior que o histórico de contratos disponível.
WITH item_usage AS (
    SELECT rci.rental_item_id,
           COUNT(*) AS usage_count,
           MAX(COALESCE(rc.pickup_date, rc.event_date))::timestamp AS last_used_at
    FROM rental_contract_items rci
    JOIN rental_contracts rc ON rc.id = rci.contract_id
    WHERE rci.rental_item_id IS NOT NULL
      AND rc.status IN ('FINALIZED', 'CLOSED')
      AND (rc.is_returned = true OR rci.is_delivered = true)
    GROUP BY rci.rental_item_id
)
UPDATE rental_items ri
SET rental_count = GREATEST(COALESCE(ri.rental_count, 0), iu.usage_count),
    last_rental_date = GREATEST(ri.last_rental_date, iu.last_used_at)
FROM item_usage iu
WHERE ri.id = iu.rental_item_id
  AND (COALESCE(ri.rental_count, 0) < iu.usage_count
       OR ri.last_rental_date IS NULL
       OR ri.last_rental_date < iu.last_used_at);
