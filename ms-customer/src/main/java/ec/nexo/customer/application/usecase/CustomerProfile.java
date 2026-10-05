package ec.nexo.customer.application.usecase;

import ec.nexo.customer.domain.model.Customer;
import ec.nexo.customer.domain.model.Preferences;

public record CustomerProfile(Customer customer, Preferences preferences) {
}
