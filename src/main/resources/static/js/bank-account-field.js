function syncBankAccountField(form) {
    const field = form.querySelector('[data-bank-account-field]');
    const account = field.querySelector('select');
    const isCash = form.elements.paymentMethod.value === 'CASH';
    field.hidden = isCash;
    account.required = !isCash;
    account.disabled = isCash;
    if (isCash) account.value = '';
}
