package ru.stepan1411.chat_images;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.stepan1411.chat_images.networking.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Chat_images implements ModInitializer {

    public static final String MOD_ID = "chat_images";
    public static final Logger LOGGER = LoggerFactory.getLogger("ChatImages");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static final Set<UUID> MODDED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<byte[]>> PENDING_IMAGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PENDING_TESTS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(HandshakeC2SPayload.TYPE, HandshakeC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ImageDataC2SPayload.TYPE, ImageDataC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ImageEndC2SPayload.TYPE, ImageEndC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TestAckC2SPayload.TYPE, TestAckC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ImageDataS2CPayload.TYPE, ImageDataS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ImageEndS2CPayload.TYPE, ImageEndS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TestS2CPayload.TYPE, TestS2CPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HandshakeC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                MODDED_PLAYERS.add(player.getUUID());
                LOGGER.info("Player {} has ChatImages mod", player.getGameProfile().name());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ImageDataC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                PENDING_IMAGES.computeIfAbsent(player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>())).add(payload.data());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ImageEndC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                int totalSize = payload.totalSize();
                List<byte[]> chunks = PENDING_IMAGES.remove(player.getUUID());
                if (chunks == null || chunks.isEmpty()) return;

                byte[] imageData = new byte[totalSize];
                int offset = 0;
                for (byte[] chunk : chunks) {
                    System.arraycopy(chunk, 0, imageData, offset, chunk.length);
                    offset += chunk.length;
                }

                broadcastImage(context.server(), player, imageData);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TestAckC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                MODDED_PLAYERS.add(player.getUUID());
                PENDING_TESTS.put(player.getUUID(), true);
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUUID();
            MODDED_PLAYERS.remove(uuid);
            PENDING_IMAGES.remove(uuid);
            PENDING_TESTS.remove(uuid);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("chatimages")
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("test")
                                    .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                                            .executes(this::executeTest)
                                    )
                            )
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reconnect")
                                    .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                                            .executes(this::executeReconnect)
                                    )
                            )
            );
        });
    }

    private int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");

        MinecraftServer server = source.getServer();
        ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);

        if (target == null) {
            source.sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        UUID targetUuid = target.getUUID();

        if (MODDED_PLAYERS.contains(targetUuid)) {
            source.sendSuccess(() -> Component.literal("§a" + playerName + " has Chat Images (handshake confirmed)"), false);
            return Command.SINGLE_SUCCESS;
        }

        PENDING_TESTS.put(targetUuid, false);
        ServerPlayNetworking.send(target, new TestS2CPayload());

        source.sendSuccess(() -> Component.literal("§7Testing " + playerName + "..."), false);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            server.execute(() -> {
                Boolean acked = PENDING_TESTS.remove(targetUuid);
                if (Boolean.TRUE.equals(acked)) {
                    source.sendSuccess(() -> Component.literal("§a" + playerName + " has Chat Images (test confirmed)"), false);
                    MODDED_PLAYERS.add(targetUuid);
                } else {
                    source.sendFailure(Component.literal("§c" + playerName + " does not have Chat Images"));
                }
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    private int executeReconnect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");

        MinecraftServer server = source.getServer();
        ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);

        if (target == null) {
            source.sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        target.connection.disconnect(Component.literal("Reconnecting to apply Chat Images changes..."));
        source.sendSuccess(() -> Component.literal("§a" + playerName + " has been disconnected"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void broadcastImage(MinecraftServer server, ServerPlayer sender, byte[] imageData) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getUUID().equals(sender.getUUID())) continue;

            if (MODDED_PLAYERS.contains(player.getUUID())) {
                sendImageToPlayer(player, sender.getUUID(), sender.getGameProfile().name(), imageData);
            } else {
                player.sendSystemMessage(Component.literal(
                        "§7[ChatImages] §f" + sender.getGameProfile().name() + " §7sent an image. Install ChatImages to view it"
                ), false);
            }
        }
    }

    private static void sendImageToPlayer(ServerPlayer player, UUID senderUuid, String senderName, byte[] imageData) {
        int offset = 0;
        while (offset < imageData.length) {
            int chunkSize = Math.min(ImageDataS2CPayload.MAX_CHUNK_SIZE, imageData.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(imageData, offset, chunk, 0, chunkSize);
            offset += chunkSize;
            ServerPlayNetworking.send(player, new ImageDataS2CPayload(senderUuid, chunk));
        }
        ServerPlayNetworking.send(player, new ImageEndS2CPayload(senderUuid, senderName, imageData.length));
    }

}
