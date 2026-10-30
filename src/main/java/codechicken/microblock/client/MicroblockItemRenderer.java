package codechicken.microblock.client;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.api.MicroMaterialClient;
import codechicken.microblock.item.MicroMaterialComponent;
import codechicken.multipart.init.CBMultipartModContent;
import codechicken.multipart.util.Sides;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

import static codechicken.microblock.CBMicroblock.MOD_ID;

/**
 * Created by covers1624 on 20/10/22.
 */
public class MicroblockItemRenderer implements SpecialModelRenderer<MicroMaterialComponent> {

    @Override
    @Nullable
    public MicroMaterialComponent extractArgument(ItemStack stack) {
        return MicroMaterialComponent.getComponent(stack);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // Just assume this item is a full block.
        // There isn't really much more we can do here without context.
        output.accept(Cuboid6.full.min.vector3f());
        output.accept(Cuboid6.full.max.vector3f());
    }

    @Override
    public void submit(@Nullable MicroMaterialComponent component, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (component == null || component.factory() == null) return;

        var clientMaterial = MicroMaterialClient.get(component.material());
        if (clientMaterial == null) return;

        var part = component.factory().create(true, component.material());
        part.setShape(component.size(), component.factory().getItemSlot());

        poseStack.pushPose();
        var offset = Vector3.CENTER.copy().subtract(part.getBounds().center());
        poseStack.translate(offset.x, offset.y, offset.z);

        var ourState = CBMultipartModContent.MULTIPART_BLOCK.get().defaultBlockState();
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();

        var cuboids = part.getRenderCuboids(true);
        List<BlockStateModelPart> list = new ArrayList<>();
        clientMaterial.collectParts(null, null, cuboids, list);
        // Each quad carries the item render type of its own material.
        Map<RenderType, List<BakedQuad>> quadsByType = new LinkedHashMap<>();
        for (var modelPart : list) {
            for (var side : Sides.SIDES_AND_NULL) {
                for (BakedQuad quad : modelPart.getQuads(side)) {
                    quadsByType.computeIfAbsent(quad.materialInfo().itemRenderType(), e -> new ArrayList<>()).add(quad);
                }
            }
        }
        quadsByType.forEach((renderType, quads) -> collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            var instance = new QuadInstance();
            instance.setLightCoords(packedLight);
            instance.setOverlayCoords(packedOverlay);
            int lastTint = -1;
            int lastTintIndex = -1;
            for (BakedQuad quad : quads) {
                int color = -1;
                int tintIndex = quad.materialInfo().tintIndex();
                if (tintIndex != -1) {
                    if (lastTintIndex != tintIndex) {
                        var tintSource = blockColors.getTintSource(ourState, tintIndex);
                        lastTint = tintSource != null ? tintSource.color(ourState) : -1;
                        lastTintIndex = tintIndex;
                    }
                    color = lastTint;
                }
                instance.setColor(ARGB.opaque(color));
                consumer.putBakedQuad(pose, quad, instance);
            }
        }));

        // TODO
//        clientMaterial.submitDynamic(part, poseStack, collector, packedLight, packedOverlay, hasFoil, outlineColor);
        poseStack.popPose();
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<MicroMaterialComponent> {

        public static final Identifier TYPE = Identifier.fromNamespaceAndPath(MOD_ID, "microblock");
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<MicroMaterialComponent> bake(BakingContext context) {
            return new MicroblockItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<MicroMaterialComponent>> type() {
            return CODEC;
        }
    }
}
