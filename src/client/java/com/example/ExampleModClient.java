package com.example;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Direction;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ExampleModClient implements ClientModInitializer {

    private boolean running = false;
    private int mode = 0;
    private int delay = 1;
    private int maxDistance = 5;
    private long lastSendTime = 0;

    private final String[] methodNames = {
        "Move Packet",
        "Interact Packet",
        "Mine Packet"
    };

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("m")
            .executes(ctx -> {
                ctx.getSource().sendFeedback(Text.literal("Available methods:"));
                for (int i = 0; i < methodNames.length; i++) {
                    ctx.getSource().sendFeedback(Text.literal(i + ": " + methodNames[i]));
                }
                return 1;
            })
            .then(literal("r")
                .then(argument("rate", IntegerArgumentType.integer(1)).executes(ctx -> {
                    delay = IntegerArgumentType.getInteger(ctx, "rate");
                    ctx.getSource().sendFeedback(Text.literal("Packet delay set to " + delay + " ticks"));
                    return 1;
                }))
            )
            .then(literal("d")
                .then(argument("distance", IntegerArgumentType.integer(1)).executes(ctx -> {
                    maxDistance = IntegerArgumentType.getInteger(ctx, "distance");
                    ctx.getSource().sendFeedback(Text.literal("Max distance set to " + maxDistance));
                    return 1;
                }))
            )
            .then(literal("stop").executes(ctx -> {
                running = false;
                ctx.getSource().sendFeedback(Text.literal("Stopped."));
                return 1;
            }))
            .then(literal("start")
                .then(argument("mode", IntegerArgumentType.integer(0, methodNames.length - 1)).executes(ctx -> {
                    mode = IntegerArgumentType.getInteger(ctx, "mode");
                    running = true;
                    ctx.getSource().sendFeedback(Text.literal("Started method: " + methodNames[mode]));
                    return 1;
                }))
            )
        );
    }

    private final Random random = Random.create();

    public ExampleModClient() {
        // Start a tick thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50); // 1 tick = 50ms
                    if (running) {
                        long now = System.currentTimeMillis();
                        if ((now - lastSendTime) >= delay * 50) {
                            lastSendTime = now;
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player == null || client.getNetworkHandler() == null) {
                                running = false;
                                continue;
                            }

                            switch (mode) {
                                case 0 -> sendMovePacket(client);
                                case 1 -> sendInteractPacket(client);
                                case 2 -> sendMinePacket(client);
                            }
                        }
                    }
                } catch (Exception e) {
                    running = false;
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendMovePacket(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double dx = (random.nextDouble() - 0.5) * 2 * maxDistance;
        double dz = (random.nextDouble() - 0.5) * 2 * maxDistance;
        double x = player.getX() + dx;
        double y = player.getY();
        double z = player.getZ() + dz;
        client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, true));
    }

    private void sendInteractPacket(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos pos = player.getBlockPos().add(
            random.nextBetween(-maxDistance, maxDistance),
            0,
            random.nextBetween(-maxDistance, maxDistance)
        );
        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(pos),
            Direction.UP,
            pos,
            false
        );
        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
    }

    private void sendMinePacket(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos pos = player.getBlockPos().add(
            random.nextBetween(-maxDistance, maxDistance),
            0,
            random.nextBetween(-maxDistance, maxDistance)
        );
        client.getNetworkHandler().sendPacket(
            new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP)
        );
    }
}
