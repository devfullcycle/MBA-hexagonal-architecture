package br.com.fullcycle.hexagonal.application.domain.customer;

import br.com.fullcycle.hexagonal.application.domain.person.Cpf;
import br.com.fullcycle.hexagonal.application.domain.person.Email;
import br.com.fullcycle.hexagonal.application.domain.person.Name;
import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;

import java.util.Objects;

public class Customer {

    private final CustomerId customerId;
    private Name name;
    private Cpf cpf;
    private Email email;

    public Customer(final CustomerId customerId, final String name, final String cpf, final String email) {
        if (customerId == null) {
            throw new ValidationException("Invalid customerId for Customer");
        }

        this.customerId = customerId;
        this.setName(name);
        this.setCpf(cpf);
        this.setEmail(email);
    }

    public static Customer newCustomer(String name, String cpf, String email) {
        return new Customer(CustomerId.unique(), name, cpf, email);
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Name name() {
        return name;
    }

    public Cpf cpf() {
        return cpf;
    }

    public Email email() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    private void setCpf(final String cpf) {
        this.cpf = new Cpf(cpf);
    }

    private void setEmail(final String email) {
        this.email = new Email(email);
    }

    private void setName(final String name) {
        this.name = new Name(name);
    }

}
