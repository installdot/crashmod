package net.example.examplemod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class ExampleModClient implements ClientModInitializer {
    private boolean running = false;
    private int mode = 0;
    private long lastSend = 0;
    private int delay = 100;
    private int maxDistance = 32;
    private final Random random = new Random();

    private final String[] methodNames = {
        "PlayerMove Packet",
        "PlayerAction Packet",
        "InteractBlock Packet"
    };

    @Override
    public void onInitializeClient() {
        registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!running || client.player == null || client.getNetworkHandler() == null) return;
            long now = System.currentTimeMillis();
            if (now - lastSend >= delay) {
                sendPacket(client);
                lastSend = now;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            running = false;
            if (client.player != null)
                client.player.sendMessage(Text.literal("Stopped spamming: Disconnected from server"), false);
        });
    }

    private void sendPacket(MinecraftClient client) {
        BlockPos pos = client.player.getBlockPos().add(
            random.nextInt(maxDistance * 2 + 1) - maxDistance,
            random.nextInt(256), // For Y axis
            random.nextInt(maxDistance * 2 + 1) - maxDistance
        );

        switch (mode) {
            case 0 -> {
                // PlayerMove Packet
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX() + random.nextInt(maxDistance * 2 + 1) - maxDistance,
                    client.player.getY(),
                    client.player.getZ() + random.nextInt(maxDistance * 2 + 1) - maxDistance,
                    true
                ));
            }
            case 1 -> {
                // PlayerAction Packet (start digging)
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    pos,
                    Direction.UP
                ));
            }
            case 2 -> {
                // InteractBlock Packet
                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    Hand.MAIN_HAND,
                    pos,
                    Direction.UP,
                    0.5f, 0.5f, 0.5f
                ));
            }
        }
    }

    private void registerCommands() {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("m")
            .executes(ctx -> {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("§6/m <id> - Start method\n§6/m r <delay_ms> - Set rate\n§6/m d <max_distance> - Set max range\n§6/m stop - Stop"), false);
                for (int i = 0; i < methodNames.length; i++) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("§7[" + i + "] " + methodNames[i]), false);
                }
                return 1;
            })
            .then(ClientCommandManager.literal("stop").executes(ctx -> {
                running = false;
                MinecraftClient.getInstance().player.sendMessage(Text.literal("§cStopped packet spam."), false);
                return 1;
            }))
            .then(ClientCommandManager.literal("r")
                .then(ClientCommandManager.argument("rate", net.minecraft.command.argument.IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        delay = net.minecraft.command.argument.IntegerArgumentType.getInteger(ctx, "rate");
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("§aRate set to " + delay + " ms"), false);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("d")
                .then(ClientCommandManager.argument("distance", net.minecraft.command.argument.IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        maxDistance = net.minecraft.command.argument.IntegerArgumentType.getInteger(ctx, "distance");
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("§aMax distance set to " + maxDistance), false);
                        return 1;
                    })))
            .then(ClientCommandManager.argument("mode", net.minecraft.command.argument.IntegerArgumentType.integer(0, methodNames.length - 1))
                .executes(ctx -> {
                    mode = net.minecraft.command.argument.IntegerArgumentType.getInteger(ctx, "mode");
                    running = true;
                    lastSend = 0;
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("§aStarted spamming: " + methodNames[mode]), false);
                    return 1;
                }))
        );
    }
}
