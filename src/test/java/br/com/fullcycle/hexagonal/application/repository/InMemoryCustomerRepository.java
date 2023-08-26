package br.com.fullcycle.hexagonal.application.repository;

import br.com.fullcycle.hexagonal.application.domain.customer.Customer;
import br.com.fullcycle.hexagonal.application.domain.customer.CustomerId;
import br.com.fullcycle.hexagonal.application.domain.person.Cpf;
import br.com.fullcycle.hexagonal.application.domain.person.Email;
import br.com.fullcycle.hexagonal.application.repositories.CustomerRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers;
    private final Map<String, Customer> customersByCPF;
    private final Map<String, Customer> customersByEmail;

    public InMemoryCustomerRepository() {
        this.customers = new HashMap<>();
        this.customersByCPF = new HashMap<>();
        this.customersByEmail = new HashMap<>();
    }

    @Override
    public Optional<Customer> customerOfId(CustomerId anId) {
        return Optional.ofNullable(this.customers.get(Objects.requireNonNull(anId).value().toString()));
    }

    @Override
    public Optional<Customer> customerOfCPF(Cpf cpf) {
        return Optional.ofNullable(this.customersByCPF.get(cpf.value()));
    }

    @Override
    public Optional<Customer> customerOfEmail(Email email) {
        return Optional.ofNullable(this.customersByEmail.get(email.value()));
    }

    @Override
    public Customer create(Customer customer) {
        this.customers.put(customer.customerId().value().toString(), customer);
        this.customersByCPF.put(customer.cpf().value(), customer);
        this.customersByEmail.put(customer.email().value(), customer);
        return customer;
    }

    @Override
    public Customer update(Customer customer) {
        this.customers.put(customer.customerId().value().toString(), customer);
        this.customersByCPF.put(customer.cpf().value(), customer);
        this.customersByEmail.put(customer.email().value(), customer);
        return customer;
    }

    @Override
    public void deleteAll() {
        this.customers.clear();
        this.customersByCPF.clear();
        this.customersByEmail.clear();
    }
}
