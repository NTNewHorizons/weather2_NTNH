package weather2.climate;

/**
 * NTNH Typed Weather Profile for a dimension.
 * Immutable record representing meteorological behavior, storm progression limits,
 * lightning odds, spawn frequencies, and block manipulation permissions.
 */
public class ClimateProfile {

    public final int dim;
    public final String name;
    public final boolean weatherEnabled;
    public final int maxStage;
    public final boolean alwaysProgresses;
    public final int deadlyCooldown;
    public final float lightningMultiplier;
    public final int landSpawnOdds;
    public final int oceanSpawnOdds;
    public final boolean grabBlocks;

    public ClimateProfile(int dim, String name, boolean weatherEnabled, int maxStage, boolean alwaysProgresses,
        int deadlyCooldown, float lightningMultiplier, int landSpawnOdds, int oceanSpawnOdds, boolean grabBlocks) {
        this.dim = dim;
        this.name = name != null ? name : ("dim_" + dim);
        this.weatherEnabled = weatherEnabled;
        this.maxStage = maxStage;
        this.alwaysProgresses = alwaysProgresses;
        this.deadlyCooldown = Math.max(0, deadlyCooldown);
        this.lightningMultiplier = Math.max(0.1F, lightningMultiplier);
        this.landSpawnOdds = Math.max(0, landSpawnOdds);
        this.oceanSpawnOdds = Math.max(0, oceanSpawnOdds);
        this.grabBlocks = grabBlocks;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(int dim) {
        return new Builder(dim);
    }

    public static Builder builder(int dim, String name) {
        return new Builder(dim, name);
    }

    public static ClimateProfile createDefault(int dim) {
        return new ClimateProfile(dim, "default_" + dim, false, 0, false, 0, 1.0F, 0, 0, false);
    }

    @Override
    public String toString() {
        return String.format(
            "Profile[%s/dim=%d, maxStage=%d, alwaysProgress=%b, cooldown=%d, lightning=%.1fx, landOdds=%d, oceanOdds=%d, grab=%b]",
            name,
            dim,
            maxStage,
            alwaysProgresses,
            deadlyCooldown,
            lightningMultiplier,
            landSpawnOdds,
            oceanSpawnOdds,
            grabBlocks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClimateProfile that = (ClimateProfile) o;
        return dim == that.dim;
    }

    @Override
    public int hashCode() {
        return dim;
    }

    /**
     * Fluent Builder for ClimateProfile with validation and range guarding.
     */
    public static class Builder {

        private int dim = -999;
        private String name = "";
        private boolean weatherEnabled = true;
        private int maxStage = 9;
        private boolean alwaysProgresses = false;
        private int deadlyCooldown = 1800;
        private float lightningMultiplier = 1.0F;
        private int landSpawnOdds = 10;
        private int oceanSpawnOdds = 0;
        private boolean grabBlocks = false;

        public Builder(String name) {
            this.name = name != null ? name : "";
            this.dim = ClimateEngine.resolveDimensionId(this.name);
        }

        public Builder(int dim) {
            this.dim = dim;
            this.name = "dim_" + dim;
        }

        public Builder(int dim, String name) {
            this.dim = dim;
            this.name = name != null ? name : ("dim_" + dim);
        }

        public Builder dim(int dim) {
            this.dim = dim;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder weatherEnabled(boolean enabled) {
            this.weatherEnabled = enabled;
            return this;
        }

        public Builder maxStage(int stage) {
            // Auto-normalize Fujita scale: 1..5 -> 5..9 (STATE_STAGE1..STATE_STAGE5)
            if (stage > 0 && stage <= 5) {
                this.maxStage = stage + 4;
            } else {
                this.maxStage = Math.max(0, Math.min(9, stage));
            }
            return this;
        }

        public Builder exactStage(int stage) {
            this.maxStage = Math.max(0, Math.min(9, stage));
            return this;
        }

        public Builder alwaysProgresses(boolean always) {
            this.alwaysProgresses = always;
            return this;
        }

        public Builder deadlyCooldown(int ticks) {
            this.deadlyCooldown = Math.max(0, ticks);
            return this;
        }

        public Builder lightningMultiplier(float mul) {
            this.lightningMultiplier = Math.max(0.1F, mul);
            return this;
        }

        public Builder landSpawnOdds(int odds) {
            this.landSpawnOdds = Math.max(0, odds);
            return this;
        }

        public Builder oceanSpawnOdds(int odds) {
            this.oceanSpawnOdds = Math.max(0, odds);
            return this;
        }

        public Builder spawnOdds(int landOdds, int oceanOdds) {
            this.landSpawnOdds = Math.max(0, landOdds);
            this.oceanSpawnOdds = Math.max(0, oceanOdds);
            return this;
        }

        public Builder grabBlocks(boolean grab) {
            this.grabBlocks = grab;
            return this;
        }

        public ClimateProfile build() {
            if (dim == -999 && name != null && !name.isEmpty()) {
                this.dim = ClimateEngine.resolveDimensionId(name);
            }
            return new ClimateProfile(
                dim,
                name,
                weatherEnabled,
                maxStage,
                alwaysProgresses,
                deadlyCooldown,
                lightningMultiplier,
                landSpawnOdds,
                oceanSpawnOdds,
                grabBlocks);
        }
    }
}
