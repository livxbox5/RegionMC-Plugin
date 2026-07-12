package org.KillerYT.regionMC.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ParticleUtil {

    // Максимальное количество частиц за один вызов
    private static final int MAX_PARTICLES_PER_CALL = 100;
    private static int particleCounter = 0;
    private static long lastResetTime = System.currentTimeMillis();

    /**
     * Сбрасывает счетчик частиц каждую секунду
     */
    private static void resetCounterIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 1000) {
            particleCounter = 0;
            lastResetTime = currentTime;
        }
    }

    /**
     * Проверяет лимит частиц
     */
    private static boolean canSpawnParticle() {
        resetCounterIfNeeded();
        return particleCounter < MAX_PARTICLES_PER_CALL;
    }

    /**
     * Вариант 1: Отображение цветных частиц с параметрами RGB
     */
    public static void displayColoredParticle(Player player, Location location, int red, int green, int blue) {
        if (!canSpawnParticle()) {
            return; // Превышен лимит частиц
        }

        try {
            // Для версий 1.20.5+ используем DUST_COLOR_TRANSITION
            player.spawnParticle(Particle.DUST_COLOR_TRANSITION, location, 1,
                    0, 0, 0, 0,
                    new Particle.DustTransition(
                            Color.fromRGB(red, green, blue),
                            Color.fromRGB(red, green, blue),
                            1.0f
                    ));
            particleCounter++;
        } catch (Exception e) {
            // Fallback для версий 1.17-1.20.4
            try {
                player.spawnParticle(Particle.DUST, location, 1,
                        0, 0, 0, 0,
                        new Particle.DustOptions(
                                Color.fromRGB(red, green, blue),
                                1.0f
                        ));
                particleCounter++;
            } catch (Exception e2) {
                // Ultimate fallback - обычные частицы без цвета
                displayFallbackParticle(player, location);
            }
        }
    }

    /**
     * Fallback метод для частиц без цвета
     */
    private static void displayFallbackParticle(Player player, Location location) {
        if (!canSpawnParticle()) {
            return;
        }

        try {
            // Попробуем различные частицы
            player.spawnParticle(Particle.FLAME, location, 1);
            particleCounter++;
        } catch (Exception e3) {
            try {
                // Используем правильное название для дыма (SMOKE вместо SMOKE_NORMAL)
                player.spawnParticle(Particle.SMOKE, location, 1);
                particleCounter++;
            } catch (Exception e4) {
                try {
                    // Используем правильное название для счастливого жителя
                    player.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
                    particleCounter++;
                } catch (Exception e5) {
                    try {
                        // Используем самые базовые частицы
                        player.spawnParticle(Particle.CRIT, location, 1);
                        particleCounter++;
                    } catch (Exception e6) {
                        // Если ничего не работает, просто игнорируем
                    }
                }
            }
        }
    }

    /**
     * Вариант 2: Отображение частиц с предустановленными цветами
     */
    public static void displayGreenParticle(Player player, Location location) {
        displayColoredParticle(player, location, 0, 255, 0);
    }

    public static void displayBlueParticle(Player player, Location location) {
        displayColoredParticle(player, location, 0, 0, 255);
    }

    public static void displayRedParticle(Player player, Location location) {
        displayColoredParticle(player, location, 255, 0, 0);
    }

    public static void displayYellowParticle(Player player, Location location) {
        displayColoredParticle(player, location, 255, 255, 0);
    }

    public static void displayCyanParticle(Player player, Location location) {
        displayColoredParticle(player, location, 0, 255, 255);
    }

    /**
     * Универсальный метод для отображения границ региона
     */
    public static void displayRegionBoundary(Player player, Location location, Color color) {
        displayColoredParticle(player, location, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Метод для отображения границ с предустановленным цветом
     */
    public static void displayRegionBoundary(Player player, Location location, String colorType) {
        switch (colorType.toLowerCase()) {
            case "green":
                displayGreenParticle(player, location);
                break;
            case "blue":
                displayBlueParticle(player, location);
                break;
            case "red":
                displayRedParticle(player, location);
                break;
            case "yellow":
                displayYellowParticle(player, location);
                break;
            case "cyan":
                displayCyanParticle(player, location);
                break;
            default:
                displayBlueParticle(player, location);
                break;
        }
    }
}