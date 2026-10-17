package net.jahus.perplayerhardcore.data;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class HardcoreData {
    public enum Mode {
        WORLDSPAWN,
        SPECTATOR;

        public static Mode fromString(String s) {
            return switch (s.toLowerCase()) {
                case "spectator", "specta" -> SPECTATOR;
                default -> WORLDSPAWN;
            };
        }

        @Override
        public String toString() {
            return this == SPECTATOR ? "spectator" : "worldspawn";
        }
    }

    private Mode mode = Mode.WORLDSPAWN;
    private int initialLives = 1;
    private int bonusLives = 0;
    private int remainingLives = 1;
    @Nullable
    private BlockPos hardcoreSpawn = null;
    private boolean pendingEnd = false;

    public HardcoreData() {}

    public HardcoreData(Mode mode, int lives) {
        this.mode = mode;
        this.initialLives = Math.max(1, lives);
        this.bonusLives = 0;
        this.remainingLives = this.initialLives;
        this.pendingEnd = false;
    }

    // Getters
    public Mode getMode() { return mode; }
    public int getInitialLives() { return initialLives; }
    public int getBonusLives() { return bonusLives; }
    public int getTotalGranted() { return initialLives + bonusLives; }
    public int getRemainingLives() { return remainingLives; }
    @Nullable
    public BlockPos getHardcoreSpawn() { return hardcoreSpawn; }
    public boolean isPendingEnd() { return pendingEnd; }

    // Setters
    public void setMode(Mode mode) { this.mode = mode; }
    public void setHardcoreSpawn(@Nullable BlockPos pos) { this.hardcoreSpawn = pos; }
    public void setPendingEnd(boolean pendingEnd) { this.pendingEnd = pendingEnd; }
    public void setRemainingLives(int remainingLives) { this.remainingLives = remainingLives; }
    public void setBonusLives(int bonusLives) { this.bonusLives = bonusLives; }

    public void addLives(int amount) {
        if (amount > 0) {
            this.bonusLives += amount;
            this.remainingLives += amount;
        }
    }

    public void removeLives(int amount) {
        if (amount > 0) {
            this.bonusLives = Math.max(0, this.bonusLives - amount);
            this.remainingLives = Math.max(0, this.remainingLives - amount);
        }
    }

    /** Called on death. Returns true if the player still has lives left. */
    public boolean onDeath() {
        if (remainingLives > 0) {
            remainingLives--;
        }
        if (remainingLives <= 0) {
            pendingEnd = true;
            return false;
        }
        return true;
    }
}
