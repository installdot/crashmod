package com.example.packetmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class PacketSpammerMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("m")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal(PacketSpamManager.listMethods()));
                    return 1;
                })
                .then(ClientCommandManager.argument("methods", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String methods = StringArgumentType.getString(ctx, "methods");
                        int started = PacketSpamManager.startMethods(methods);
                        ctx.getSource().sendFeedback(Text.literal("Started " + started + " method(s)."));
                        return started;
                    })
                )
        );
    }
}
