// JsonMessageUtil.java
package org.KillerYT.regionMC.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class JsonMessageUtil {

    /**
     * Объединяет несколько компонентов с разделителем
     */
    public static Component joinComponents(List<Component> components, Component separator) {
        if (components == null || components.isEmpty()) {
            return Component.empty();
        }

        Component result = components.get(0);
        for (int i = 1; i < components.size(); i++) {
            result = result.append(separator).append(components.get(i));
        }
        return result;
    }

    /**
     * Объединяет несколько компонентов с разделителем (массив)
     */
    public static Component joinComponents(Component[] components, Component separator) {
        if (components == null || components.length == 0) {
            return Component.empty();
        }

        Component result = components[0];
        for (int i = 1; i < components.length; i++) {
            result = result.append(separator).append(components[i]);
        }
        return result;
    }

    /**
     * Создает компонент для подстановки команды в чат
     */
    public static Component createSuggestComponent(String text, String hoverText, String command) {
        Component component = Component.text(text);

        if (hoverText != null) {
            component = component.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        }

        if (command != null) {
            component = component.clickEvent(ClickEvent.suggestCommand(command));
        }

        return component;
    }

    /**
     * Создает компонент для выполнения команды
     */
    public static Component createRunComponent(String text, String hoverText, String command) {
        Component component = Component.text(text);

        if (hoverText != null) {
            component = component.hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        }

        if (command != null) {
            component = component.clickEvent(ClickEvent.runCommand(command));
        }

        return component;
    }

    /**
     * Отправляет несколько компонентов в одной строке
     */
    public static void sendCompositeMessage(Player player, Component... components) {
        Component combined = Component.empty();
        for (Component component : components) {
            combined = combined.append(component);
        }
        player.sendMessage(combined);
    }

    // ==================== ОСНОВНЫЕ КНОПКИ ====================

    /**
     * Pos1 - ВЫПОЛНЯЕТСЯ сразу
     */
    public static Component createSetPos1Button() {
        return createRunComponent(
                "§b[🎯 Set Pos1]",
                "§7Установить первую позицию здесь",
                "/rg pos1"
        );
    }

    /**
     * Pos2 - ВЫПОЛНЯЕТСЯ сразу
     */
    public static Component createSetPos2Button() {
        return createRunComponent(
                "§b[🎯 Set Pos2]",
                "§7Установить вторую позицию здесь",
                "/rg pos2"
        );
    }

    /**
     * Expand - ПОДСТАВЛЯЕТ команду в чат (ГАРАНТИРОВАННО!)
     */
    public static Component createExpandButton() {
        // ГАРАНТИРОВАННО используем suggestCommand
        Component component = Component.text("§e[⬆ Expand]");
        component = component.hoverEvent(HoverEvent.showText(Component.text("§7Нажмите чтобы вставить команду /rg expand")));
        component = component.clickEvent(ClickEvent.suggestCommand("/rg expand "));
        return component;
    }

    /**
     * Claim - ПОДСТАВЛЯЕТ команду в чат
     */
    public static Component createClaimButton() {
        return createSuggestComponent(
                "§a[□ Create Region]",
                "§7Нажмите чтобы вставить команду /rg claim\n§7Затем введите имя региона",
                "/rg claim "
        );
    }

    /**
     * Clear - ВЫПОЛНЯЕТСЯ сразу
     */
    public static Component createClearButton() {
        return createRunComponent(
                "§c[🗑 Clear]",
                "§7Очистить позиции",
                "/rg clear"
        );
    }

    /**
     * Help - ВЫПОЛНЯЕТСЯ сразу
     */
    public static Component createHelpButton() {
        return createRunComponent(
                "§6[? Help]",
                "§7Показать справку по командам",
                "/rg help"
        );
    }

    /**
     * Info - ВЫПОЛНЯЕТСЯ сразу (для текущего региона)
     */
    public static Component createRegionInfoButton(String regionName) {
        return createRunComponent(
                "§a[📋 Info]",
                "§7Показать информацию о регионе " + regionName,
                "/rg info " + regionName
        );
    }

    /**
     * Remove Member - ПОДСТАВЛЯЕТ команду в чат (регион уже подставлен)
     */
    public static Component createRemoveMemberButton(String regionName) {
        return createSuggestComponent(
                "§c[➖ Remove Member]",
                "§7Удалить участника из региона " + regionName,
                "/rg removemember " + regionName + " "
        );
    }

    /**
     * Remove Owner - ПОДСТАВЛЯЕТ команду в чат (регион уже подставлен)
     */
    public static Component createRemoveOwnerButton(String regionName) {
        return createSuggestComponent(
                "§c[➖ Remove Owner]",
                "§7Удалить владельца из региона " + regionName,
                "/rg removeowner " + regionName + " "
        );
    }

    /**
     * Add Member - ПОДСТАВЛЯЕТ команду в чат (регион уже подставлен)
     */
    public static Component createAddMemberButton(String regionName) {
        return createSuggestComponent(
                "§a[➕ Add Member]",
                "§7Добавить участника в регион " + regionName,
                "/rg addmember " + regionName + " "
        );
    }

    /**
     * Add Owner - ПОДСТАВЛЯЕТ команду в чат (регион уже подставлен)
     */
    public static Component createAddOwnerButton(String regionName) {
        return createSuggestComponent(
                "§a[➕ Add Owner]",
                "§7Добавить владельца в регион " + regionName,
                "/rg addowner " + regionName + " "
        );
    }

    // ==================== ДОПОЛНИТЕЛЬНЫЕ КНОПКИ ====================

    /**
     * Кнопка для быстрого расширения (ПОДСТАВЛЯЕТ команду с параметрами)
     */
    public static Component createQuickExpandSuggestButton(int blocks, String direction) {
        return createSuggestComponent(
                "§e[⬆ Expand " + direction + "]",
                "§7Нажмите чтобы вставить команду расширения на " + blocks + " блоков " + direction,
                "/rg expand " + blocks + " " + direction
        );
    }

    /**
     * Кнопка для быстрого создания (ВЫПОЛНЯЕТСЯ сразу)
     */
    public static Component createQuickCreateButton(String regionName) {
        return createRunComponent(
                "§a[🏷 Create]",
                "§7Создать регион " + regionName,
                "/rg claim " + regionName
        );
    }

    /**
     * Кнопка для создания другого региона (ПОДСТАВЛЯЕТ в чат)
     */
    public static Component createCreateAnotherButton() {
        return createSuggestComponent(
                "§6[🔄 Create Another]",
                "§7Создать другой регион",
                "/rg claim "
        );
    }

    // ==================== ГОТОВЫЕ МЕНЮ ====================

    /**
     * Основное меню управления позициями (как на скриншоте)
     */
    public static void sendPositionMenu(Player player, boolean hasPos1, boolean hasPos2) {
        // Статус позиций
        String status = "§7Статус: ";
        status += hasPos1 ? "§aPos1✓ " : "§cPos1✗ ";
        status += hasPos2 ? "§aPos2✓" : "§cPos2✗";
        player.sendMessage(Component.text(status));

        // Кнопки управления позициями (как на скриншоте)
        Component posLine = Component.empty()
                .append(createSetPos1Button())
                .append(Component.text(" "))
                .append(createSetPos2Button())
                .append(Component.text(" "))
                .append(createHelpButton())
                .append(Component.text(" "))
                .append(createClearButton());

        player.sendMessage(posLine);

        // Если обе позиции установлены - показываем кнопки создания и расширения
        if (hasPos1 && hasPos2) {
            player.sendMessage(Component.text("§a✓ Обе позиции установлены! Доступные действия:"));

            Component actionLine = Component.empty()
                    .append(createClaimButton())    // ПОДСТАВЛЯЕТ /rg claim
                    .append(Component.text(" "))
                    .append(createExpandButton());  // ПОДСТАВЛЯЕТ /rg expand

            player.sendMessage(actionLine);
        }
    }

    /**
     * Простое меню: [Create Region] [Expand] [Clear]
     */
    public static void sendSimpleMenu(Player player) {
        Component line = Component.empty()
                .append(createClaimButton())        // ПОДСТАВЛЯЕТ /rg claim
                .append(Component.text(" "))
                .append(createExpandButton())       // ПОДСТАВЛЯЕТ /rg expand
                .append(Component.text(" "))
                .append(createClearButton());       // ВЫПОЛНЯЕТ /rg clear

        player.sendMessage(line);
    }

    /**
     * Меню после установки позиций
     */
    public static void sendAfterPositionMenu(Player player) {
        player.sendMessage(Component.text("§6=== Позиции установлены ==="));

        Component line = Component.empty()
                .append(createClaimButton())        // ПОДСТАВЛЯЕТ /rg claim
                .append(Component.text(" "))
                .append(createExpandButton())       // ПОДСТАВЛЯЕТ /rg expand
                .append(Component.text(" "))
                .append(createClearButton());       // ВЫПОЛНЯЕТ /rg clear

        player.sendMessage(line);
        player.sendMessage(Component.text("§7Нажмите §a[Create Region]§7 чтобы создать регион"));
        player.sendMessage(Component.text("§7Нажмите §e[Expand]§7 чтобы расширить регион"));
    }

    /**
     * Меню расширения региона (ТОЛЬКО ПОДСТАВЛЯЕТ команду)
     */
    public static void sendExpandSuggestMenu(Player player) {
        player.sendMessage(Component.text("§6=== Расширение региона ==="));
        player.sendMessage(Component.text("§7Нажмите кнопку чтобы вставить команду:"));

        Component line = Component.empty()
                .append(createExpandButton());  // ГАРАНТИРОВАННО ПОДСТАВЛЯЕТ

        player.sendMessage(line);
        player.sendMessage(Component.text("§7Затем введите: §e<количество_блоков> <up|down>"));
        player.sendMessage(Component.text("§7Пример: §e10 up§7 или §e5 down"));
        player.sendMessage(Component.text("§7И нажмите §aEnter§7 для выполнения"));

        // Быстрые кнопки расширения (ТОЛЬКО ПОДСТАВЛЯЮТ!)
        player.sendMessage(Component.text("§7Быстрые шаблоны:"));
        Component quickLine = Component.empty()
                .append(createQuickExpandSuggestButton(5, "up"))
                .append(Component.text(" "))
                .append(createQuickExpandSuggestButton(10, "up"))
                .append(Component.text(" "))
                .append(createQuickExpandSuggestButton(5, "down"))
                .append(Component.text(" "))
                .append(createQuickExpandSuggestButton(10, "down"));

        player.sendMessage(quickLine);
        player.sendMessage(Component.text("§7Нажмите на шаблон и затем Enter для выполнения"));
    }

    /**
     * Меню расширения региона
     */
    public static void sendExpandMenu(Player player) {
        sendExpandSuggestMenu(player); // Используем тот же метод
    }

    /**
     * Меню создания региона
     */
    public static void sendClaimMenu(Player player) {
        player.sendMessage(Component.text("§6=== Создание региона ==="));
        player.sendMessage(Component.text("§7Нажмите кнопку чтобы вставить команду:"));

        Component line = Component.empty()
                .append(createClaimButton());   // ПОДСТАВЛЯЕТ

        player.sendMessage(line);
        player.sendMessage(Component.text("§7Затем введите имя региона и нажмите Enter"));
    }

    /**
     * Меню управления регионом (после создания)
     */
    public static void sendRegionManagementMenu(Player player, String regionName) {
        player.sendMessage(Component.text("§6=== Управление регионом " + regionName + " ==="));

        // Основные действия
        Component mainActions = Component.empty()
                .append(createRegionInfoButton(regionName))  // ВЫПОЛНЯЕТ
                .append(Component.text(" "))
                .append(createExpandButton());               // ПОДСТАВЛЯЕТ

        player.sendMessage(mainActions);

        // Управление участниками
        player.sendMessage(Component.text("§7Управление участниками:"));
        Component memberActions = Component.empty()
                .append(createAddMemberButton(regionName))       // ПОДСТАВЛЯЕТ
                .append(Component.text(" "))
                .append(createRemoveMemberButton(regionName));   // ПОДСТАВЛЯЕТ

        player.sendMessage(memberActions);

        // Управление владельцами
        player.sendMessage(Component.text("§7Управление владельцами:"));
        Component ownerActions = Component.empty()
                .append(createAddOwnerButton(regionName))        // ПОДСТАВЛЯЕТ
                .append(Component.text(" "))
                .append(createRemoveOwnerButton(regionName));    // ПОДСТАВЛЯЕТ

        player.sendMessage(ownerActions);
    }

    /**
     * Меню успешного создания региона
     */
    public static void sendSuccessMenu(Player player, String regionName) {
        player.sendMessage(Component.text("§6=== Регион создан! ==="));
        player.sendMessage(Component.text("§a✓ Регион '§e" + regionName + "§a' успешно создан"));

        Component line = Component.empty()
                .append(createRegionInfoButton(regionName))  // ВЫПОЛНЯЕТ
                .append(Component.text(" "))
                .append(createAddMemberButton(regionName))   // ПОДСТАВЛЯЕТ
                .append(Component.text(" "))
                .append(createCreateAnotherButton());        // ПОДСТАВЛЯЕТ

        player.sendMessage(line);
    }

    /**
     * Быстрое меню действий
     */
    public static void sendQuickActionMenu(Player player) {
        Component line = Component.empty()
                .append(createClaimButton())
                .append(Component.text(" "))
                .append(createExpandButton())
                .append(Component.text(" "))
                .append(createClearButton());

        player.sendMessage(line);
    }
}