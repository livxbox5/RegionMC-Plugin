package org.KillerYT.regionMC.flags.main;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public abstract class AbstractRegionFlag<T> {
    private final String name;
    private final T defaultValue;

    public AbstractRegionFlag(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    // Абстрактные методы, которые должны быть реализованы
    public abstract T parseInput(String input);
    public abstract boolean isAllowed(T value);
    public abstract String getDescription();  // ← ДОБАВИТЬ ЭТУ СТРОКУ

    // Вспомогательные методы для преобразования значений
    protected Boolean convertToBoolean(String value) {
        if (value == null) return null;

        String lowerValue = value.toLowerCase();
        return switch (lowerValue) {
            case "true", "allow", "yes", "1", "on", "разрешить" -> true;
            case "false", "deny", "no", "0", "off", "запретить" -> false;
            default -> null;
        };
    }

    protected boolean isDenied(String value) {
        Boolean result = convertToBoolean(value);
        return result != null && !result;
    }

    protected boolean isAllowed(String value) {
        Boolean result = convertToBoolean(value);
        return result != null && result;
    }
}