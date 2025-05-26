package com.example.packetmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.*;

public class PacketMod implements ModInitializer {
    private static final Set<Integer> ALL_METHODS = Set.of(1, 2, 3, 4, 5);

    @Override
    public void onInitialize() {
        PacketMethods.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("m")
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal("Available Methods:"));
                    ctx.getSource().sendMessage(Text.literal("1 - RANDOMPACKETA"));
                    ctx.getSource().sendMessage(Text.literal("2 - RANDOMPACKETB"));
                    ctx.getSource().sendMessage(Text.literal("3 - CLICKEXC"));
                    ctx.getSource().sendMessage(Text.literal("4 - TABNBT"));
                    ctx.getSource().sendMessage(Text.literal("5 - CONSOLSPAM"));
                    ctx.getSource().sendMessage(Text.literal("Use /m <id> [<id2> ...] to spam multiple."));
                    ctx.getSource().sendMessage(Text.literal("Use /m all to spam all."));
                    ctx.getSource().sendMessage(Text.literal("Use /m stop to stop all."));
                    return 1;
                })
                .then(argument("args", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String argStr = ctx.getArgument("args", String.class).toLowerCase();
                        if (argStr.equals("stop")) {
                            PacketMethods.stop();
                            ctx.getSource().sendMessage(Text.literal("Stopped all spamming."));
                        } else if (argStr.equals("all")) {
                            PacketMethods.runMethods(ALL_METHODS);
                            ctx.getSource().sendMessage(Text.literal("Started spamming ALL methods."));
                        } else {
                            // Parse IDs separated by spaces, ignore invalid ones
                            String[] parts = argStr.split("\\s+");
                            Set<Integer> ids = new HashSet<>();
                            for (String part : parts) {
                                try {
                                    int id = Integer.parseInt(part);
                                    if (ALL_METHODS.contains(id)) {
                                        ids.add(id);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                            if (ids.isEmpty()) {
                                ctx.getSource().sendMessage(Text.literal("No valid method IDs specified."));
                            } else {
                                PacketMethods.runMethods(ids);
                                ctx.getSource().sendMessage(Text.literal("Started spamming methods: " + ids));
                            }
                        }
                        return 1;
                    })
                )
            );
        });
    }
}
