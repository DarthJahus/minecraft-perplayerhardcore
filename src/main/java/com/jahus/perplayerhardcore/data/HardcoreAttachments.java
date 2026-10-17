package com.jahus.perplayerhardcore.data;

import com.jahus.perplayerhardcore.PerPlayerHardcore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class HardcoreAttachments {

    public static final Codec<HardcoreData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("mode").forGetter(d -> d.getMode().toString()),
                    Codec.INT.fieldOf("initial_lives").forGetter(HardcoreData::getInitialLives),
                    Codec.INT.fieldOf("bonus_lives").forGetter(HardcoreData::getBonusLives),
                    Codec.INT.fieldOf("remaining_lives").forGetter(HardcoreData::getRemainingLives),
                    Codec.BOOL.fieldOf("pending_end").forGetter(HardcoreData::isPendingEnd),
                    BlockPos.CODEC.optionalFieldOf("hardcore_spawn").forGetter(d -> Optional.ofNullable(d.getHardcoreSpawn()))
            ).apply(instance, (modeStr, initial, bonus, remaining, pending, spawnOpt) -> {
                HardcoreData data = new HardcoreData(HardcoreData.Mode.fromString(modeStr), initial);
                // Force exact values
                data.setRemainingLives(remaining);
                data.setBonusLives(bonus);
                data.setPendingEnd(pending);
                spawnOpt.ifPresent(data::setHardcoreSpawn);
                return data;
            })
    );

    public static final AttachmentType<HardcoreData> HARDCORE_DATA =
            AttachmentRegistry.createPersistent(
                    Identifier.fromNamespaceAndPath(PerPlayerHardcore.MOD_ID, "hardcore_data"),
                    CODEC
            );

    public static void register() {
        PerPlayerHardcore.LOGGER.info("Hardcore data attachment registered.");
    }

    @Nullable
    public static HardcoreData get(ServerPlayer player) {
        return player.getAttached(HARDCORE_DATA);
    }

    public static void set(ServerPlayer player, HardcoreData data) {
        player.setAttached(HARDCORE_DATA, data);
    }

    public static void remove(ServerPlayer player) {
        player.removeAttached(HARDCORE_DATA);
    }

    public static boolean isTracked(ServerPlayer player) {
        return player.hasAttached(HARDCORE_DATA);
    }
}
