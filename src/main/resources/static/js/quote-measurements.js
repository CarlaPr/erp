(function (root) {
    'use strict';

    function measurements(item) {
        const values = Array.isArray(item.measurements) && item.measurements.length ? item.measurements
            : (Number(item.width) > 0 && Number(item.height) > 0 ? [{ width: item.width, height: item.height }] : []);
        return values.map(measure => ({ ...measure,
            quantity: Number(measure.quantity ?? item.quantity ?? 1),
            unitPrice: Number(measure.unitPrice ?? item.unitPrice ?? 0)
        }));
    }

    function area(item) {
        return measurements(item).reduce((total, measure) => total + Number(measure.width) * Number(measure.height), 0);
    }

    function totalArea(item) {
        return measurements(item).reduce((total, measure) => total + Number(measure.width) * Number(measure.height) * measure.quantity, 0);
    }

    function subtotal(item) {
        const values = measurements(item);
        return values.length ? values.reduce((total, measure) => total + Number(measure.width) * Number(measure.height) * measure.quantity * measure.unitPrice, 0)
            : Number(item.quantity ?? 1) * Number(item.unitPrice ?? 0);
    }

    function hasPerMeasurementPricing(item) {
        return Array.isArray(item.measurements) && item.measurements.some(measure => measure.quantity != null || measure.unitPrice != null);
    }

    function pricingQuantity(item) {
        return hasPerMeasurementPricing(item) ? 1 : Number(item.quantity ?? 1);
    }

    function unitTotal(item) {
        return hasPerMeasurementPricing(item) ? subtotal(item) : (area(item) || 1) * Number(item.unitPrice ?? 0);
    }

    function formatNumber(value) {
        return Number(value).toLocaleString('pt-BR', { maximumFractionDigits: 4 });
    }

    root.QuoteMeasurements = { measurements, area, totalArea, subtotal, hasPerMeasurementPricing, pricingQuantity, unitTotal, formatNumber };
})(typeof window !== 'undefined' ? window : globalThis);
