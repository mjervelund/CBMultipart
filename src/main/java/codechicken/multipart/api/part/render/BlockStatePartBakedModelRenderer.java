package codechicken.multipart.api.part.render;

import codechicken.multipart.api.part.BlockStateModelPart;
import codechicken.multipart.util.ProxyBlockAndTintGetter;
import codechicken.multipart.util.WrappedBlockModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.neoforged.neoforge.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link StaticPartRenderer} implementation to render {@link BlockStateModelPart}'s {@link BlockStateModel}.
 *
 * @see StaticPartRenderer
 * Created by covers1624 on 7/11/21.
 */
public class BlockStatePartBakedModelRenderer<T extends BlockStateModelPart> implements StaticPartRenderer<T> {

    @Override
    public void collectParts(ModelData modelData, BlockAndTintGetter level, BlockPos pos, RandomSource rand, List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts) {
        var data = modelData.get(BlockStateModelPart.BlockStateModelPartData.TYPE);
        if (data == null) return;

        var state = data.state();
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        // TODO we can likely cache the list used for this collectParts call somewhere if we use an instance class?
        // TODO we need to rewrite tint...
        List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> modelParts = new ArrayList<>();
        model.collectParts(
                new ProxyBlockAndTintGetter(level, pos, state, modelData),
                pos,
                state,
                rand,
                modelParts
        );
        for (var modelPart : modelParts) {
            parts.add(new WrappedBlockModelPart(modelPart, state));
        }
    }
}
