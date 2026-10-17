package net.jahus.perplayerhardcore.mixin;

import net.jahus.perplayerhardcore.config.ModConfig;
import net.jahus.perplayerhardcore.data.HardcoreAttachments;
import net.jahus.perplayerhardcore.data.HardcoreData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    private static MutableComponent prefix() {
        return Component.literal(ModConfig.prefix).withStyle(ChatFormatting.RED);
    }

    private static ChatFormatting colorForLives(int remaining) {
        if (remaining <= 1) return ChatFormatting.RED;
        if (remaining == 2) return ChatFormatting.GOLD;
        return ChatFormatting.GREEN;
    }

    private static String livesWord(int n) {
        return n == 1 ? ModConfig.life : ModConfig.lives;
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) return;

        boolean stillAlive = data.onDeath();

        if (stillAlive) {
            int rem = data.getRemainingLives();
            MutableComponent msg = prefix().append(
                    Component.literal(String.format(ModConfig.livesLeft, rem, livesWord(rem)))
                            .withStyle(colorForLives(rem))
            );
            player.sendSystemMessage(msg);

            BlockPos hardcoreSpawn = data.getHardcoreSpawn();
            if (hardcoreSpawn != null) {
                LevelData.RespawnData respawnData = LevelData.RespawnData.of(
                        player.level().dimension(),
                        hardcoreSpawn,
                        player.getYRot(),
                        0.0f
                );
                ServerPlayer.RespawnConfig config = new ServerPlayer.RespawnConfig(respawnData, true);
                player.setRespawnPosition(config, false);
            }
        } else {
            MutableComponent msg = prefix().append(
                    Component.literal(ModConfig.gameOver).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );
            player.sendSystemMessage(msg);

            if (data.getMode() == HardcoreData.Mode.WORLDSPAWN) {
                player.setRespawnPosition(null, false);
            }
        }

        HardcoreAttachments.set(player, data);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void afterRespawn(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        HardcoreData data = HardcoreAttachments.get(oldPlayer);
        if (data == null) {
            data = HardcoreAttachments.get(player);
            if (data == null) return;
        }

        if (data.isPendingEnd()) {
            HardcoreAttachments.remove(player);
            HardcoreAttachments.remove(oldPlayer);

            if (data.getMode() == HardcoreData.Mode.SPECTATOR) {
                player.setGameMode(GameType.SPECTATOR);
                player.sendSystemMessage(
                        prefix().append(Component.literal(ModConfig.spectator).withStyle(ChatFormatting.RED))
                );
            } else {
                ServerLevel overworld = player.level().getServer().overworld();
                BlockPos worldSpawn = overworld.getRespawnData().pos();

                double distSq = player.distanceToSqr(
                        worldSpawn.getX() + 0.5,
                        worldSpawn.getY(),
                        worldSpawn.getZ() + 0.5
                );
                if (distSq > 16.0) {
                    player.teleportTo(overworld,
                            worldSpawn.getX() + 0.5,
                            worldSpawn.getY(),
                            worldSpawn.getZ() + 0.5,
                            java.util.Set.of(),
                            player.getYRot(),
                            player.getXRot(),
                            true);
                }
            }
        } else {
            HardcoreAttachments.set(player, data);

            BlockPos hardcoreSpawn = data.getHardcoreSpawn();
            if (hardcoreSpawn != null) {
                ServerLevel level = (ServerLevel) player.level();
                level.getServer().execute(() -> {
                    if (player.isAlive() && !player.hasDisconnected()) {
                        double distSq = player.distanceToSqr(
                                hardcoreSpawn.getX() + 0.5,
                                hardcoreSpawn.getY(),
                                hardcoreSpawn.getZ() + 0.5
                        );
                        if (distSq > 4.0) {
                            player.teleportTo(level,
                                    hardcoreSpawn.getX() + 0.5,
                                    hardcoreSpawn.getY(),
                                    hardcoreSpawn.getZ() + 0.5,
                                    java.util.Set.of(),
                                    player.getYRot(),
                                    player.getXRot(),
                                    true);
                        }
                    }
                });
            }
        }
    }
}
