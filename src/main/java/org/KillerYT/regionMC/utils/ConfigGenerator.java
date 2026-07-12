package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigGenerator {
    private final RegionMC plugin;

    public ConfigGenerator(RegionMC plugin) {
        this.plugin = plugin;
    }

    public void generateAllConfigs() {
        plugin.getLogger().info("=== CONFIG GENERATION STARTED ===");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        copyAllResources();

        plugin.getLogger().info("=== CONFIG GENERATION COMPLETED ===");
    }

    private void copyAllResources() {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            if (jarFile.isDirectory()) {
                copyFromDirectory(jarFile);
            } else {
                copyFromJar(jarFile);
            }
        } catch (URISyntaxException e) {
            plugin.getLogger().warning("Failed to get JAR path: " + e.getMessage());
            copyFromClassLoader();
        }
    }

    private void copyFromJar(File jarFile) {
        plugin.getLogger().info("Copying resources from JAR...");

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            int createdCount = 0;
            int updatedCount = 0;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Пропускаем Java-классы
                if (shouldSkipEntry(name)) {
                    continue;
                }

                // Пропускаем если это не конфигурационный файл
                if (!isConfigFile(name)) {
                    continue;
                }

                File destFile = new File(plugin.getDataFolder(), name);

                // ВАЖНО: Создаём родительские папки
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (parent.mkdirs()) {
                        plugin.getLogger().info("  Created folder: " + parent.getName());
                    }
                }

                // Проверяем, это папка или файл
                if (name.endsWith("/")) {
                    // Это папка - создаём если не существует
                    if (!destFile.exists() && destFile.mkdirs()) {
                        plugin.getLogger().info("  Created folder: " + name);
                    }
                    continue;
                }

                // Копируем или обновляем файл
                try (InputStream in = jar.getInputStream(entry)) {
                    if (!destFile.exists()) {
                        copyFile(in, destFile);
                        createdCount++;
                        plugin.getLogger().info("  Created: " + name);
                    } else {
                        boolean updated = updateFileIfNeeded(in, destFile, name);
                        if (updated) {
                            updatedCount++;
                            plugin.getLogger().info("  Updated: " + name);
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to process: " + name + " - " + e.getMessage());
                }
            }

            plugin.getLogger().info("✓ Created: " + createdCount + ", Updated: " + updatedCount);

            // Дополнительно создаём папку regions (если не существует)
            File regionsFolder = new File(plugin.getDataFolder(), "regions");
            if (!regionsFolder.exists()) {
                regionsFolder.mkdirs();
                plugin.getLogger().info("  Created folder: regions");
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read JAR: " + e.getMessage());
            copyFromClassLoader();
        }
    }

    /**
     * Проверяет, является ли entry конфигурационным файлом/папкой
     */
    private boolean isConfigFile(String name) {
        // Пропускаем системные файлы
        if (name.equals("plugin.yml") || name.equals("paper-plugin.yml")) {
            return false;
        }

        // Всегда обрабатываем YAML файлы
        if (name.endsWith(".yml") || name.endsWith(".yaml")) {
            return true;
        }

        // Обрабатываем папки
        if (name.endsWith("/")) {
            return true;
        }

        // Обрабатываем конкретные файлы
        if (name.equals("config.yml")) return true;
        if (name.equals("ColorUtils.yml")) return true;
        if (name.equals("permission.yml")) return true;
        if (name.equals("Placeholder.yml")) return true;

        return false;
    }

    private boolean updateFileIfNeeded(InputStream jarInputStream, File destFile, String fileName) {
        try {
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                return updateYamlFile(jarInputStream, destFile);
            } else {
                return updateBinaryFile(jarInputStream, destFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update: " + fileName + " - " + e.getMessage());
            return false;
        }
    }

    private boolean updateYamlFile(InputStream jarInputStream, File destFile) {
        try {
            YamlConfiguration jarConfig = new YamlConfiguration();
            jarConfig.load(new InputStreamReader(jarInputStream));

            YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(destFile);

            Set<String> jarKeys = getAllKeys(jarConfig);
            Set<String> existingKeys = getAllKeys(existingConfig);

            Set<String> newKeys = new HashSet<>(jarKeys);
            newKeys.removeAll(existingKeys);

            if (!newKeys.isEmpty()) {
                for (String key : newKeys) {
                    Object value = jarConfig.get(key);
                    existingConfig.set(key, value);
                }
                existingConfig.save(destFile);
                return true;
            }

            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update YAML: " + destFile.getName() + " - " + e.getMessage());
            return false;
        }
    }

    private Set<String> getAllKeys(YamlConfiguration config) {
        Set<String> keys = new HashSet<>();
        if (config.getKeys(true) != null) {
            keys.addAll(config.getKeys(true));
        }
        return keys;
    }

    private boolean updateBinaryFile(InputStream jarInputStream, File destFile) {
        try {
            ByteArrayOutputStream jarBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                jarBuffer.write(buffer, 0, bytesRead);
            }
            byte[] jarContent = jarBuffer.toByteArray();

            byte[] existingContent = Files.readAllBytes(destFile.toPath());

            if (!Arrays.equals(jarContent, existingContent)) {
                Files.write(destFile.toPath(), jarContent);
                return true;
            }

            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update binary: " + destFile.getName() + " - " + e.getMessage());
            return false;
        }
    }

    private void copyFile(InputStream in, File destFile) throws IOException {
        try (OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private boolean shouldSkipEntry(String name) {
        if (name.endsWith(".class")) return true;
        if (name.startsWith("org/") || name.startsWith("org\\")) return true;
        if (name.contains("META-INF")) return true;
        return false;
    }

    private void copyFromDirectory(File rootDir) {
        plugin.getLogger().info("IDE mode - copying from: " + rootDir.getPath());

        File resourcesDir = new File(rootDir.getParentFile(), "resources");
        if (!resourcesDir.exists()) {
            resourcesDir = new File("src/main/resources");
        }
        if (!resourcesDir.exists()) {
            plugin.getLogger().warning("Resources folder not found!");
            return;
        }

        copyFolderRecursive(resourcesDir, plugin.getDataFolder());
    }

    private void copyFolderRecursive(File source, File dest) {
        if (source == null || !source.exists()) return;

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.getName().equals("org") && file.isDirectory()) continue;

            File destFile = new File(dest, file.getName());

            if (file.isDirectory()) {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                    plugin.getLogger().info("  Created folder: " + destFile.getName());
                }
                copyFolderRecursive(file, destFile);
            } else {
                if (!destFile.exists()) {
                    try (InputStream in = new FileInputStream(file);
                         OutputStream out = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        plugin.getLogger().info("  Created: " + destFile.getName());
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to copy: " + file.getName());
                    }
                }
            }
        }
    }

    private void copyFromClassLoader() {
        plugin.getLogger().info("Fallback: copying from ClassLoader...");

        // Создаём папки
        createFolder("lang");
        createFolder("Settings");
        createFolder("region");
        createFolder("regions");

        // Копируем файлы
        String[] resources = {
                "config.yml",
                "ColorUtils.yml",
                "permission.yml",
                "Placeholder.yml",
                "lang/ru.yml",
                "lang/en.yml",
                "Settings/main.yml",
                "Settings/limit.yml"
        };

        for (String path : resources) {
            try (InputStream in = plugin.getResource(path)) {
                if (in == null) continue;

                File destFile = new File(plugin.getDataFolder(), path);
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                if (!destFile.exists()) {
                    copyFile(in, destFile);
                    plugin.getLogger().info("✓ Created: " + path);
                } else if (path.endsWith(".yml")) {
                    updateYamlFile(in, destFile);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy: " + path);
            }
        }
    }

    private void createFolder(String folderName) {
        File folder = new File(plugin.getDataFolder(), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.getLogger().info("✓ Created folder: " + folderName);
        }
    }

    private String getRelativePath(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath)) {
            return filePath.substring(rootPath.length() + 1);
        }
        return file.getName();
    }

    public void forceUpdateAllFiles() {
        plugin.getLogger().warning("Force updating all files from JAR!");

        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isDirectory()) {
                plugin.getLogger().warning("Cannot force update in IDE mode");
                return;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                int updatedCount = 0;

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (shouldSkipEntry(name)) continue;
                    if (!isConfigFile(name)) continue;

                    File destFile = new File(plugin.getDataFolder(), name);
                    File parent = destFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    if (!name.endsWith("/")) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            copyFile(in, destFile);
                            updatedCount++;
                            plugin.getLogger().info("  Force updated: " + name);
                        }
                    }
                }

                plugin.getLogger().info("✓ Force updated " + updatedCount + " files");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to force update: " + e.getMessage());
        }
    }
}