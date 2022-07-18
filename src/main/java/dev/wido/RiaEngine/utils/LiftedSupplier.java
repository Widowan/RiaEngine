package dev.wido.RiaEngine.utils;

public interface LiftedSupplier<T> {
    T get() throws Exception;
}
