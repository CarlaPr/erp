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
        return summary(item).subtotal;
    }

    function summary(item) {
        const rows = unitRows(item);
        return {
            rows,
            totalArea: rows.reduce((total, row) => total + row.totalArea, 0),
            subtotal: rows.reduce((total, row) => total + row.subtotal, 0)
        };
    }

    function unitRows(item) {
        const values = measurements(item);
        if (!values.length) {
            const quantity = Number(item.quantity ?? 1);
            const unitPrice = Number(item.unitPrice ?? 0);
            return [{ width: 0, height: 0, area: 0, totalArea: 0, quantity, unitPrice, subtotal: quantity * unitPrice }];
        }
        return values.map(measure => {
            const width = Number(measure.width);
            const height = Number(measure.height);
            const area = width * height;
            const quantity = measure.quantity;
            const unitPrice = area * measure.unitPrice;
            return { width, height, area, totalArea: area * quantity, quantity, unitPrice, subtotal: quantity * unitPrice };
        });
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

    root.QuoteMeasurements = { measurements, area, totalArea, subtotal, summary, unitRows, hasPerMeasurementPricing, pricingQuantity, unitTotal, formatNumber };
})(typeof window !== 'undefined' ? window : globalThis);
