package br.com.fullcycle.hexagonal.application;

public abstract class UnitUseCase<INPUT> {

    public abstract void execute(INPUT input);
}
