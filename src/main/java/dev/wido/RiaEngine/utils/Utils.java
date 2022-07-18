package dev.wido.RiaEngine.utils;

import java.util.Optional;

public class Utils {
    public static <T> Optional<T> checkedLift(LiftedSupplier<T> f) {
        try {
            return Optional.ofNullable(f.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
