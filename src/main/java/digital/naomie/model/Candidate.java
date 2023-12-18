package digital.naomie.model;

import net.minecraft.util.math.BlockPos;

public class Candidate {
    public BlockPos pos;
    public int distance;
    public boolean processed;

    public Candidate(BlockPos pos, int distance, boolean processed) {
        this.pos = pos;
        this.distance = distance;
        this.processed = processed;
    }
}
