package org.smeshikMods.macedamageui.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;

public class MacedamageuiClient implements ClientModInitializer {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private static float currentDamage = 0;
    private static boolean isHoldingMace = false;
    private static float fallBonus = 0;

    @Override
    public void onInitializeClient() {
        // Регистрируем обработчик тиков для расчета урона
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ItemStack mainHand = client.player.getMainHandStack();
            isHoldingMace = mainHand.getItem() == Items.MACE;

            if (isHoldingMace) {
                currentDamage = calculateMaceDamage(client.player, mainHand);
            } else {
                currentDamage = 0;
                fallBonus = 0;
            }
        });

        // Регистрируем отрисовку HUD
        HudRenderCallback.EVENT.register(this::renderDamageIndicator);
    }

    private float calculateMaceDamage(net.minecraft.entity.player.PlayerEntity player, ItemStack mace) {
        // Базовый урон
        float baseDamage = 5.0F;

        // Атрибут атаки игрока
        float attackDamage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        // Зачарования
        int sharpness = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, mace);
        int smite = EnchantmentHelper.getLevel(Enchantments.SMITE, mace);
        int bane = EnchantmentHelper.getLevel(Enchantments.BANE_OF_ARTHROPODS, mace);
        int density = EnchantmentHelper.getLevel(Registries.ENCHANTMENT.get(new Identifier("density")), mace);

        float enchantBonus = 0;
        if (sharpness > 0) {
            enchantBonus += 0.5F * sharpness + 0.5F;
        } else if (smite > 0) {
            enchantBonus += 2.5F * smite;
        } else if (bane > 0) {
            enchantBonus += 2.5F * bane;
        }

        // Бонус от падения
        float fallDistance = player.fallDistance;
        fallBonus = 0;
        if (fallDistance > 3) { // Минимальная высота для бонуса
            fallBonus = Math.min((fallDistance - 3) * 2, 40);

            if (density > 0) {
                fallBonus += density * 2;
            }

            if (fallBonus > 0) {
                baseDamage = 7.0F; // Увеличенный базовый урон при падении
            }
        }

        // Эффекты
        float strengthMultiplier = 1.0F;
        if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int strengthLevel = player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
            strengthMultiplier += 0.3F * strengthLevel;
        }

        if (player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int weaknessLevel = player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1;
            strengthMultiplier -= 0.2F * weaknessLevel;
        }

        // Итоговый урон
        float totalDamage = (baseDamage + attackDamage - 1 + enchantBonus) * strengthMultiplier + fallBonus;

        // Критический удар
        if (player.fallDistance > 0 && !player.isOnGround() && !player.isClimbing() && !player.isTouchingWater()) {
            totalDamage *= 1.5F;
        }

        return Math.max(0, totalDamage);
    }

    private void renderDamageIndicator(DrawContext context, float tickDelta) {
        if (!isHoldingMace || currentDamage <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Позиция справа от прицела
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int textX = centerX + 20;
        int textY = centerY - 4;

        // Цвет в зависимости от урона
        Formatting color = Formatting.WHITE;
        if (currentDamage >= 30) color = Formatting.RED;
        else if (currentDamage >= 20) color = Formatting.GOLD;
        else if (currentDamage >= 15) color = Formatting.YELLOW;
        else if (currentDamage >= 10) color = Formatting.GREEN;

        // Текст урона
        String damageText = "⚔ " + FORMAT.format(currentDamage);
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(damageText).formatted(color, Formatting.BOLD),
                textX, textY, 0xFFFFFF
        );

        // Дополнительная информация о падении
        if (fallBonus > 0) {
            int fallTextY = textY + 12;
            String fallText = "↑ +" + FORMAT.format(fallBonus) + " от падения";
            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(fallText).formatted(Formatting.GRAY),
                    textX, fallTextY, 0xFFFFFF
            );
        }
    }
}