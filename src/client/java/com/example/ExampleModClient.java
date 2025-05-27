package com.example;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExampleModClient implements ClientModInitializer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final Set<Integer> activeModes = new HashSet<>();
    private static int packetRate = 5;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("m")
                        .executes(ctx -> {
                            printModes();
                            return 1;
                        })
                        .then(ClientCommandManager.literal("stop")
                                .executes(ctx -> {
                                    stopSpamming();
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("r")
                                .then(ClientCommandManager.argument("rate", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            packetRate = IntegerArgumentType.getInteger(ctx, "rate");
                                            mc.player.sendMessage(Text.literal("Rate set to " + packetRate), false);
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("all")
                                .executes(ctx -> {
                                    startSpamming(Set.of(1, 2));
                                    return 1;
                                }))
                        .then(ClientCommandManager.argument("mode", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> {
                                    int mode = IntegerArgumentType.getInteger(ctx, "mode");
                                    startSpamming(Set.of(mode));
                                    return 1;
                                }))));
    }

    private void printModes() {
        mc.player.sendMessage(Text.literal("1. Spam Sign Random"), false);
        mc.player.sendMessage(Text.literal("2. Spam Sign Near Me"), false);
        mc.player.sendMessage(Text.literal("/m <1|2|all> to start, /m stop to stop, /m r <rate> to set rate"), false);
    }

    private void startSpamming(Set<Integer> modes) {
        stopSpamming();
        activeModes.clear();
        activeModes.addAll(modes);
        running.set(true);
        mc.player.sendMessage(Text.literal("Spamming started with modes: " + activeModes), false);

        new Thread(() -> {
            while (running.get()) {
                for (int i = 0; i < packetRate; i++) {
                    if (!running.get()) break;

                    if (activeModes.contains(1)) sendRandomSignPacket();
                    if (activeModes.contains(2)) sendNearbySignPackets();
                }
                try {
                    Thread.sleep(50); // ~1 tick (20 TPS)
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void stopSpamming() {
        running.set(false);
        activeModes.clear();
        mc.player.sendMessage(Text.literal("Spamming stopped."), false);
    }

    private void sendRandomSignPacket() {
        if (mc.player == null) return;
        Random rand = new Random();
        BlockPos pos = mc.player.getBlockPos().add(rand.nextInt(1000) - 500, rand.nextInt(256), rand.nextInt(1000) - 500);
        sendSignPacket(pos);
    }

    private void sendNearbySignPackets() {
        if (mc.player == null) return;
        BlockPos base = mc.player.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = base.add(dx, dy, dz);
                    sendSignPacket(pos);
                }
            }
        }
    }

    private void sendSignPacket(BlockPos pos) {
        if (mc.getNetworkHandler() != null) {
            ClientPlayerEntity player = mc.player;
            if (player != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(pos, true,
                        "bella", "bella", "bella", "bella"));
            }
        }
    }
}
