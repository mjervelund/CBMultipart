package codechicken.microblock.client;

import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.vec.Line3;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.init.CBMultipartModContent;
import codechicken.multipart.util.Sides;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Created by covers1624 on 7/20/26.
 */
public class MicroblockPlacementOutlineRenderer implements CustomBlockOutlineRenderer {

    private final Vector3 hit;
    private final int side;
    private final List<Line3> lines;
    private final @Nullable PlacementPreview preview;

    public MicroblockPlacementOutlineRenderer(Vector3 hit, int side, List<Line3> lines, @Nullable PlacementPreview preview) {
        this.hit = hit;
        this.side = side;
        this.lines = lines;
        this.preview = preview;
    }

    @Override
    public boolean render(BlockOutlineRenderState renderState, SubmitNodeCollector collector, PoseStack poseStack, LevelRenderState levelRenderState) {
        var camera = levelRenderState.cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        renderPlacementGrid(poseStack, collector);
        renderPlacementPreview(poseStack, collector);
        poseStack.popPose();

        return true;
    }

    private void renderPlacementGrid(PoseStack poseStack, SubmitNodeCollector collector) {
        // TODO high contrast?
        var lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            Matrix4 mat = new Matrix4(pose);
            transformFace(hit, side, mat);
            VertexConsumer cons = new TransformingVertexConsumer(buffer, mat);
            for (Line3 line : lines) {
                bufferLinePair(cons, line.pt1, line.pt2, 0F, 0F, 0F, 0.4f, lineWidth);
            }
        });
    }

    private void bufferLinePair(VertexConsumer builder, Vector3 v1, Vector3 v2, float r, float g, float b, float a, float lineWidth) {
        Vector3 vn = v1.copy().subtract(v2);
        double d = vn.mag();
        vn.divide(d);
        builder.addVertex((float) v1.x, (float) v1.y, (float) v1.z).setColor(r, g, b, a).setNormal((float) vn.x, (float) vn.y, (float) vn.z).setLineWidth(lineWidth);
        builder.addVertex((float) v2.x, (float) v2.y, (float) v2.z).setColor(r, g, b, a).setNormal((float) vn.x, (float) vn.y, (float) vn.z).setLineWidth(lineWidth);
    }

    private void transformFace(Vector3 hit, int side, Matrix4 mat) {
        Vector3 pos = hit.copy().floor().add(Vector3.CENTER);
        mat.translate(pos);
        mat.apply(Rotation.sideRotations[side]);
        Vector3 rHit = pos.copy().subtract(hit).apply(Rotation.sideRotations[side ^ 1].inverse());
        mat.translate(0, rHit.y - 0.002, 0);
    }

    private void renderPlacementPreview(PoseStack poseStack, SubmitNodeCollector collector) {
        if (preview == null) return;

        poseStack.pushPose();
        poseStack.translate(preview.pos.getX(), preview.pos.getY(), preview.pos.getZ());
        // Ordered after the grid lines so the translucent preview blends over them.
        collector.order(1).submitCustomGeometry(poseStack, Sheets.translucentBlockItemSheet(), (pose, buffer) -> renderPreviewParts(pose, buffer, preview.parts));
        poseStack.popPose();
    }

    private static void renderPreviewParts(PoseStack.Pose pose, VertexConsumer consumer, List<BlockStateModelPart> parts) {
        var blockColors = Minecraft.getInstance().getBlockColors();
        var ourState = CBMultipartModContent.MULTIPART_BLOCK.get().defaultBlockState();
        int lastTint = -1;
        int lastTintIndex = -1;
        var instance = new QuadInstance();
        instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        for (var modelPart : parts) {
            for (var side : Sides.SIDES_AND_NULL) {
                for (BakedQuad quad : modelPart.getQuads(side)) {
                    int color = -1;
                    if (quad.materialInfo().tintIndex() != -1) {
                        if (lastTintIndex != quad.materialInfo().tintIndex()) {
                            var tintSource = blockColors.getTintSource(ourState, quad.materialInfo().tintIndex());
                            lastTint = tintSource != null ? tintSource.color(ourState) : -1;
                            lastTintIndex = quad.materialInfo().tintIndex();
                        }
                        color = lastTint;
                    }
                    instance.setColor(ARGB.color(0.4F, color));
                    consumer.putBakedQuad(pose, quad, instance);
                }
            }
        }
    }

    public record PlacementPreview(BlockPos pos, List<BlockStateModelPart> parts) { }
}
