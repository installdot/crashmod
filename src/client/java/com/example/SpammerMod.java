package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SpammerMod implements ClientModInitializer {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private volatile boolean enabled = false;
    private int rate = 1; // packets per tick
    private final Random random = new Random();

    private final Set<Integer> activeMethods = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientCommandManager.DISPATCHER.register(literal("m")
            .executes(ctx -> {
                sendMessage("Available methods:");
                sendMessage("1. Spam Sign (random locations)");
                sendMessage("2. Spam Sign Near Me (radius 3)");
                sendMessage("Use /m <num> to start, /m stop to stop, /m r <rate> to set rate.");
                return 1;
            })
            .then(ClientCommandManager.argument("args", IntegerArgumentType.greedyInt())
                .executes(ctx -> {
                    var args = IntegerArgumentType.getInteger(ctx, "args");
                    // Not used because greedyInt is just one int; We'll parse differently below.
                    return 1;
                })
            )
            .then(ClientCommandManager.argument("method", IntegerArgumentType.integer(1, 2))
                .executes(ctx -> {
                    int method = IntegerArgumentType.getInteger(ctx, "method");
                    activeMethods.clear();
                    activeMethods.add(method);
                    enabled = true;
                    sendMessage("Started method " + method);
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("stop")
                .executes(ctx -> {
                    enabled = false;
                    activeMethods.clear();
                    sendMessage("Stopped all spamming");
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("all")
                .executes(ctx -> {
                    activeMethods.clear();
                    activeMethods.add(1);
                    activeMethods.add(2);
                    enabled = true;
                    sendMessage("Started all methods");
                    return 1;
                })
            )
            .then(ClientCommandManager.literal("r")
                .then(ClientCommandManager.argument("rate", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        rate = IntegerArgumentType.getInteger(ctx, "rate");
                        sendMessage("Set spam rate to " + rate + " packets per tick");
                        return 1;
                    })
                )
            )
        );

        // Run the spam logic on client tick
        mc.execute(() -> {
            mc.getNetworkHandler().addListener(() -> {
                mc.execute(() -> {
                    mc.getTickScheduler().scheduleRepeating(() -> {
                        if (enabled) {
                            for (int method : activeMethods) {
                                for (int i = 0; i < rate; i++) {
                                    switch (method) {
                                        case 1 -> spamSignRandom();
                                        case 2 -> spamSignNear();
                                    }
                                }
                            }
                        }
                    }, 1);
                });
            });
        });
    }

    private void spamSignRandom() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        BlockPos pos = new BlockPos(random.nextInt(300) - 150, random.nextInt(256), random.nextInt(300) - 150);
        sendUpdateSignPacket(pos, "bella", "bella", "bella", "bella");
    }

    private void spamSignNear() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    sendUpdateSignPacket(pos, "bella", "bella", "bella", "bella");
                }
            }
        }
    }

    private void sendUpdateSignPacket(BlockPos pos, String line1, String line2, String line3, String line4) {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(pos, false, line1, line2, line3, line4));
    }

    private void sendMessage(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("[SpammerMod] " + msg), false);
        }
    }
}
