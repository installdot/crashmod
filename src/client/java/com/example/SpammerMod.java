package com.example.spammermod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.ArgumentType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class SpammerMod implements ClientModInitializer {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    // State
    private final Set<Integer> activeMethods = new HashSet<>();
    private int rate = 1; // packets per tick
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void onInitializeClient() {
        registerCommands();
        startSpammerThread();
    }

    private void registerCommands() {
        ClientCommandManager.DISPATCHER.register(
            literal("m")
            .executes(context -> {
                sendUsage();
                return 1;
            })
            .then(argument("args", StringArgumentType.greedyString())
                .executes(context -> {
                    String input = StringArgumentType.getString(context, "args").toLowerCase(Locale.ROOT);
                    handleCommand(input);
                    return 1;
                })
            )
        );
    }

    private void sendUsage() {
        mc.player.sendMessage(Text.literal("Available methods:"));
        mc.player.sendMessage(Text.literal("1. Spam Sign (random locations)"));
        mc.player.sendMessage(Text.literal("2. Spam Sign Near Me (3-block radius)"));
        mc.player.sendMessage(Text.literal("3. Spam ClickExc"));
        mc.player.sendMessage(Text.literal("4. Spam Invalid Packet (placeholder)"));
        mc.player.sendMessage(Text.literal("/m <num> [num2 ...] - run methods"));
        mc.player.sendMessage(Text.literal("/m all - run all methods"));
        mc.player.sendMessage(Text.literal("/m stop - stop all"));
        mc.player.sendMessage(Text.literal("/m r <rate> - set packets per tick"));
    }

    private void handleCommand(String input) {
        if (input.equals("stop")) {
            running.set(false);
            activeMethods.clear();
            mc.player.sendMessage(Text.literal("Stopped all spam methods."));
            return;
        }

        if (input.equals("all")) {
            activeMethods.clear();
            activeMethods.addAll(Arrays.asList(1, 2, 3, 4));
            running.set(true);
            mc.player.sendMessage(Text.literal("Running all methods."));
            return;
        }

        if (input.startsWith("r ")) {
            String[] parts = input.split(" ");
            if (parts.length == 2) {
                try {
                    int newRate = Integer.parseInt(parts[1]);
                    if (newRate <= 0) throw new NumberFormatException();
                    rate = newRate;
                    mc.player.sendMessage(Text.literal("Set packets per tick rate to " + rate));
                } catch (NumberFormatException e) {
                    mc.player.sendMessage(Text.literal("Invalid rate number."));
                }
            } else {
                mc.player.sendMessage(Text.literal("Usage: /m r <rate>"));
            }
            return;
        }

        // Parse numbers for methods
        String[] tokens = input.split(" ");
        Set<Integer> methods = new HashSet<>();
        for (String t : tokens) {
            try {
                int num = Integer.parseInt(t);
                if (num >= 1 && num <= 4) {
                    methods.add(num);
                } else {
                    mc.player.sendMessage(Text.literal("Method number " + num + " is out of range."));
                    return;
                }
            } catch (NumberFormatException e) {
                mc.player.sendMessage(Text.literal("Invalid method number: " + t));
                return;
            }
        }

        if (!methods.isEmpty()) {
            activeMethods.clear();
            activeMethods.addAll(methods);
            running.set(true);
            mc.player.sendMessage(Text.literal("Running methods: " + activeMethods));
        } else {
            sendUsage();
        }
    }

    private void startSpammerThread() {
        Thread thread = new Thread(() -> {
            while (true) {
                if (running.get()) {
                    for (int i = 0; i < rate; i++) {
                        for (int method : activeMethods) {
                            runMethod(method);
                        }
                    }
                }
                try {
                    Thread.sleep(50); // 20 ticks per second = 50 ms per tick approx
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void runMethod(int method) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();

        switch (method) {
            case 1: // Spam Sign at random positions within ±1000 blocks radius
                int rx = random.nextInt(2001) - 1000;
                int ry = random.nextInt(256);
                int rz = random.nextInt(2001) - 1000;
                BlockPos randomPos = new BlockPos(rx, ry, rz);
                networkHandler.sendPacket(new UpdateSignC2SPacket(randomPos, false, "bella", "bella", "bella", "bella"));
                break;

            case 2: // Spam Sign Near Me in 3-block radius cube
                BlockPos playerPos = mc.player.getBlockPos();
                int radius = 3;
                for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
                    for (int y = playerPos.getY() - radius; y <= playerPos.getY() + radius; y++) {
                        for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            // Optional: check if it's a sign block before sending
                            if (mc.world.getBlockState(pos).isOf(Blocks.OAK_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.SPRUCE_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.BIRCH_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.JUNGLE_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.ACACIA_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.DARK_OAK_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.CRIMSON_SIGN) ||
                                mc.world.getBlockState(pos).isOf(Blocks.WARPED_SIGN)) {
                                networkHandler.sendPacket(new UpdateSignC2SPacket(pos, false, "bella", "bella", "bella", "bella"));
                            }
                        }
                    }
                }
                break;

            case 3: // Spam ClickExc (simulate window click)
                if (mc.player.currentScreenHandler != null) {
                    int actionId = mc.player.currentScreenHandler.getNextActionId(mc.player);
                    networkHandler.sendPacket(new ClickWindowC2SPacket(mc.player.currentScreenHandler.syncId, 0, 0, ClickWindowC2SPacket.ClickType.PICKUP, mc.player.getMainHandStack(), (short) actionId));
                }
                break;

            case 4: // Spam Invalid Packet - Placeholder: send invalid click window (invalid slot)
                if (mc.player.currentScreenHandler != null) {
                    int actionId = mc.player.currentScreenHandler.getNextActionId(mc.player);
                    networkHandler.sendPacket(new ClickWindowC2SPacket(mc.player.currentScreenHandler.syncId, -999, 0, ClickWindowC2SPacket.ClickType.PICKUP, new ItemStack(Items.APPLE), (short) actionId));
                }
                break;
        }
    }
}
