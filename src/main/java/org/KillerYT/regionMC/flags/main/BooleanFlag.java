package org.KillerYT.regionMC.flags.main;

import lombok.Getter;

@Getter
public class BooleanFlag extends AbstractRegionFlag<Boolean> {
    private final String description;
    private final boolean inverted;

    public BooleanFlag(String name, Boolean defaultValue, String description) {
        this(name, defaultValue, description, false);
    }

    public BooleanFlag(String name, Boolean defaultValue, String description, boolean inverted) {
        super(name, defaultValue);
        this.description = description;
        this.inverted = inverted;
    }

    @Override
    public Boolean parseInput(String input) {
        if (input == null) return getDefaultValue();

        String lower = input.toLowerCase();

        return switch (lower) {
            case "разрешить", "разрешено", "да", "true", "yes", "on", "1", "allow", "allowed" -> true;
            case "запретить", "запрещено", "нет", "false", "no", "off", "0", "deny", "denied" -> false;
            default -> getDefaultValue();
        };
    }

    @Override
    public boolean isAllowed(Boolean value) {
        boolean actualValue = value != null ? value : getDefaultValue();

        if (inverted) {
            // Инвертированная логика: true → false, false → true
            return !actualValue;
        } else {
            // Прямая логика: true → true, false → false
            return actualValue;
        }
    }
}