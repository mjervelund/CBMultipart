package codechicken.multipart.util;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Created by covers1624 on 7/18/26.
 */
public class WrappedBlockModelPart implements BlockStateModelPart {

    private final BlockStateModelPart other;
    private final BlockState state;

    public WrappedBlockModelPart(BlockStateModelPart other, BlockState state) {
        this.other = other;
        this.state = state;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return other.getQuads(direction);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return other.useAmbientOcclusion();
    }

    @Override
    public Material.Baked particleMaterial() {
        return other.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return other.materialFlags();
    }

    @Override
    public TriState ambientOcclusion() {
        return other.ambientOcclusion();
    }
}
