package com.craftingdead.core.client.renderer.entity;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.vehicle.T72B3Entity;
import com.craftingdead.core.world.vehicle.VehicleContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renders the native T-72B3 model without any vehicle-mod dependency. */
public final class T72B3Renderer extends EntityRenderer<T72B3Entity> {

  private static final double BODY_RENDER_DISTANCE_SQR = 96.0D * 96.0D;
  private static final double TURRET_RENDER_DISTANCE_SQR = 32.0D * 32.0D;

  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "textures/entity/vehicle/t_72b3.png");
  private static final ResourceLocation[] TEXTURES = {
      TEXTURE,
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/vehicle/t_72b3_tan.png"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/vehicle/t_72b3_z.png"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/vehicle/t_72b3_v.png"),
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/entity/vehicle/t_72b3_x.png")
  };
  private static final ResourceLocation BODY_MODEL = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "models/vehicle/t_72b3_body.obj");
  private static final ResourceLocation TURRET_MODEL = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "models/vehicle/t_72b3_turret.obj");
  private static final ResourceLocation TRACK_MODEL = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "models/vehicle/t_72b3_track.obj");
  private static final ResourceLocation TRACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      CraftingDead.ID, "textures/entity/vehicle/t_72b3_track.png");
  private static final float BODY_MIN_Y = 0.102468F;
  private static final float BODY_CENTER_Z = 0.0723195F;
  private static final double[][] LEFT_WHEEL_PIVOTS = {
      {1.731D, 1.0313D, 4.0D}, {1.731D, 0.53977D, 3.119D},
      {1.731D, 0.53977D, 2.044D}, {1.731D, 0.53977D, 1.0315D},
      {1.731D, 0.53335D, 0.0D}, {1.731D, 0.53335D, -1.0435D},
      {1.731D, 0.53335D, -2.0622D}, {1.731D, 0.90625D, -3.047D},
      {1.731D, 1.1875D, -1.5315D}, {1.731D, 1.1875D, 0.5D},
      {1.731D, 1.1875D, 2.5625D}
  };
  private static final double[][] RIGHT_WHEEL_PIVOTS = {
      {-1.731D, 1.0313D, 4.0D}, {-1.731D, 0.53977D, 3.119D},
      {-1.731D, 0.53977D, 2.044D}, {-1.731D, 0.53977D, 1.0315D},
      {-1.731D, 0.53335D, 0.0D}, {-1.731D, 0.53335D, -1.0435D},
      {-1.731D, 0.53335D, -2.0622D}, {-1.731D, 0.90625D, -3.047D},
      {-1.731D, 1.1875D, -1.5315D}, {-1.731D, 1.1875D, 0.5D},
      {-1.731D, 1.1875D, 2.5625D}
  };
  // The old vehicle runtime generated a continuous tread from its ground_tread path. These are
  // the same wheel envelope points expressed in the imported model's Y/Z coordinates.
  private static final double[][] TREAD_LOOP = {
      {0.95D, 4.45D}, {0.20D, 3.80D}, {0.03D, 3.15D},
      {0.02D, -2.15D}, {0.32D, -3.25D}, {0.90D, -3.55D},
      {1.38D, -3.25D}, {1.52D, -2.65D}, {1.52D, 3.25D}
  };
  private static final double TREAD_SPACING = 0.25D;

  private final T72B3ObjModel body;
  private final T72B3ObjModel headlights;
  private final T72B3ObjModel driverHatch;
  private final T72B3ObjModel turret;
  private final T72B3ObjModel mainGun;
  private final T72B3ObjModel gunTube;
  private final T72B3ObjModel slatArmour;
  private final T72B3ObjModel commanderCupola;
  private final T72B3ObjModel commanderHatch;
  private final T72B3ObjModel shooterHatch;
  private final T72B3ObjModel loaderHatch;
  private final T72B3ObjModel shellMechanism;
  private final T72B3ObjModel treadLink;
  private final T72B3ObjModel[] leftWheels = new T72B3ObjModel[11];
  private final T72B3ObjModel[] rightWheels = new T72B3ObjModel[11];
  private final Map<T72B3Entity, VehicleVisualState> visualStates = new WeakHashMap<>();

  public T72B3Renderer(EntityRendererProvider.Context context) {
    super(context);
    // This object is a pre-positioned copy of b3_turret embedded in the body OBJ. The original MTS
    // runtime hides/replaces it with the separately mounted gun part; rendering both made two
    // turrets overlap whenever the aim changed.
    this.body = T72B3ObjModel.load(BODY_MODEL, staticBodyExclusions());
    this.headlights = T72B3ObjModel.loadOnly(BODY_MODEL, "&headlights");
    this.driverHatch = T72B3ObjModel.loadOnly(BODY_MODEL, "door_d");
    for (int index = 0; index < 11; index++) {
      this.leftWheels[index] = T72B3ObjModel.loadOnly(BODY_MODEL, "lw" + index);
      this.rightWheels[index] = T72B3ObjModel.loadOnly(BODY_MODEL, "rw" + index);
    }
    this.turret = T72B3ObjModel.load(TURRET_MODEL, "commander_hatch", "door_commander",
        "door_shooter", "door_shatch", "xueta", "main_gun", "gun", "#reshetka_1488");
    this.mainGun = T72B3ObjModel.loadOnly(TURRET_MODEL, "main_gun");
    this.gunTube = T72B3ObjModel.loadOnly(TURRET_MODEL, "gun");
    this.slatArmour = T72B3ObjModel.loadOnly(TURRET_MODEL, "#reshetka_1488");
    this.commanderCupola = T72B3ObjModel.loadOnly(TURRET_MODEL, "commander_hatch");
    this.commanderHatch = T72B3ObjModel.loadOnly(TURRET_MODEL, "door_commander");
    this.shooterHatch = T72B3ObjModel.loadOnly(TURRET_MODEL, "door_shooter");
    this.loaderHatch = T72B3ObjModel.loadOnly(TURRET_MODEL, "door_shatch");
    this.shellMechanism = T72B3ObjModel.loadOnly(TURRET_MODEL, "xueta");
    this.treadLink = T72B3ObjModel.load(TRACK_MODEL);
    this.shadowRadius = 5.0F;
  }

  @Override
  public void render(T72B3Entity tank, float entityYaw, float partialTick, PoseStack poseStack,
      MultiBufferSource buffer, int packedLight) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null && minecraft.player.getVehicle() == tank
        && tank.isWeaponController(minecraft.player) && minecraft.options.keyUse.isDown()) {
      // The original interior camera suppresses the vehicle skin around its optic. Without this,
      // the imported high-detail turret is drawn between the camera and the outside world.
      return;
    }
    // The original LAV meshes are deliberately detailed. Keep the complete vehicle nearby,
    // but avoid submitting its turret vertices for tanks that are far from the camera.
    double distanceSqr = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
        .distanceToSqr(tank.position());
    if (distanceSqr > BODY_RENDER_DISTANCE_SQR) {
      return;
    }
    VehicleVisualState visual = this.visualStates.computeIfAbsent(tank,
        ignored -> new VehicleVisualState());
    visual.update(tank, entityYaw, partialTick);
    boolean renderTurret = distanceSqr <= TURRET_RENDER_DISTANCE_SQR;
    poseStack.pushPose();
    // The LAV OBJ already points along Minecraft's +Z forward axis. Applying the usual extra
    // 180-degree model correction made the visible bow, seats and movement direction disagree.
    poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
    poseStack.translate(0.0D, visual.suspensionOffset, 0.0D);
    poseStack.mulPose(Axis.ZP.rotationDegrees(visual.bodyRoll));
    poseStack.mulPose(Axis.XP.rotationDegrees(visual.bodyPitch));
    // The source definition rocks the complete chassis on a main-gun discharge. Keep the
    // displacement small enough that the collision body stays authoritative while preserving
    // the visible 50-ton suspension response.
    if (!tank.isDestroyed() && visual.recoil > 0.001F) {
      float hullKick = visual.recoil * visual.recoil;
      poseStack.translate(0.0D, hullKick * 0.035D, -hullKick * 0.055D);
      poseStack.mulPose(Axis.XP.rotationDegrees(-hullKick * 1.05F));
    }
    ResourceLocation tankTexture = this.getTextureLocation(tank);
    var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(tankTexture));
    poseStack.scale(1.25F, 1.25F, 1.25F);
    poseStack.translate(0.0D, -BODY_MIN_Y, -BODY_CENTER_Z);
    if (tank.isDestroyed()) {
      this.body.render(vertexConsumer, poseStack.last(), packedLight, 82, 86, 78, 255);
    } else {
      this.body.render(vertexConsumer, poseStack.last(), packedLight);
    }
    this.headlights.render(vertexConsumer, poseStack.last(),
        tank.areHeadlightsOn() ? 0x00F000F0 : packedLight);
    this.renderYawHatch(poseStack, vertexConsumer, packedLight, this.driverHatch,
        -0.43815D, 1.788D, 2.1645D, visual.driverHatchAngle);
    this.renderWheelSet(poseStack, vertexConsumer, packedLight, this.leftWheels,
        LEFT_WHEEL_PIVOTS, visual.leftWheelRotation);
    this.renderWheelSet(poseStack, vertexConsumer, packedLight, this.rightWheels,
        RIGHT_WHEEL_PIVOTS, visual.rightWheelRotation);
    if (tank.isLeftTrackInstalled()) {
      this.renderTread(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TRACK_TEXTURE)),
          packedLight, 1.731D, visual.leftWheelRotation, false);
    }
    if (tank.isRightTrackInstalled()) {
      this.renderTread(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TRACK_TEXTURE)),
          packedLight, -1.731D, visual.rightWheelRotation, false);
    }
    if (this.shouldRenderTrackBlueprint(tank)) {
      var previewConsumer = buffer.getBuffer(RenderType.entityTranslucent(TRACK_TEXTURE));
      if (!tank.isLeftTrackInstalled()) {
        this.renderTread(poseStack, previewConsumer, packedLight, 1.731D, 0.0F, true);
      }
      if (!tank.isRightTrackInstalled()) {
        this.renderTread(poseStack, previewConsumer, packedLight, -1.731D, 0.0F, true);
      }
    }
    boolean turretBlueprint = this.shouldRenderTurretBlueprint(tank);
    if (renderTurret && (tank.isTurretInstalled() || turretBlueprint)) {
      poseStack.pushPose();
      // Original LAV definition: the B3 turret is installed at this body-relative anchor.
      poseStack.translate(0.0D, 1.7185D, 0.1968D);
      if (tank.isDestroyed()) {
        // Once the thrown turret has landed, freeze its transform. Continuing to apply the
        // server-synchronised wreck timer made it rock between network snapshots indefinitely.
        float wreckTime = Math.min(56.0F, tank.getWreckAge() + partialTick);
        float launch = Math.min(1.0F, wreckTime / 24.0F);
        float fall = Math.max(0.0F, (wreckTime - 24.0F) / 32.0F);
        poseStack.translate(0.0D, Math.max(0.0D, launch * 3.8D - fall * fall * 3.8D), 0.0D);
        float settle = Mth.clamp(fall, 0.0F, 1.0F);
        settle = settle * settle * (3.0F - 2.0F * settle);
        poseStack.mulPose(Axis.YP.rotationDegrees(tank.getWreckTurretYaw() * settle));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(settle,
            wreckTime * 7.5F, tank.getWreckTurretRoll())));
      }
      poseStack.mulPose(Axis.YP.rotationDegrees(-visual.turretYaw));
      var turretConsumer = turretBlueprint && !tank.isTurretInstalled()
          ? buffer.getBuffer(RenderType.entityTranslucent(tankTexture)) : vertexConsumer;
      if (turretBlueprint && !tank.isTurretInstalled()) {
        this.turret.render(turretConsumer, poseStack.last(), packedLight, 70, 205, 255, 105);
      } else if (tank.isDestroyed()) {
        this.turret.render(turretConsumer, poseStack.last(), packedLight, 72, 76, 70, 255);
      } else {
        this.turret.render(turretConsumer, poseStack.last(), packedLight);
      }
      this.renderElevatingGun(poseStack, turretConsumer, packedLight,
          visual.turretPitch + visual.reloadDip,
          visual.recoil, turretBlueprint && !tank.isTurretInstalled(), tank.isDestroyed());
      if (tank.isTurretInstalled()) {
        // These two OBJ objects form one composite lid. Keep their closed-pose relationship and
        // rotate them through one shared transform; animating them in opposite directions leaves
        // two vertical plates trapping the occupant between them.
        this.renderCommanderHatchAssembly(poseStack, vertexConsumer, packedLight,
            visual.commanderHatchAngle, tank.isDestroyed());
        // Restore the screen-left lid to the source model's original fore/aft animation.
        this.renderHatch(poseStack, vertexConsumer, packedLight, this.shooterHatch,
            -0.651D, 0.83245D, -0.377D, visual.shooterHatchAngle, tank.isDestroyed());
        this.renderHatch(poseStack, vertexConsumer, packedLight, this.loaderHatch,
            0.51195D, 0.92355D, -0.84197D, visual.loaderHatchAngle, tank.isDestroyed());
        this.renderShellMechanism(poseStack, vertexConsumer, packedLight,
            visual.loaderHatchProgress);
        if (tank.isSlatArmourInstalled()) {
          if (tank.isDestroyed()) {
            this.slatArmour.render(vertexConsumer, poseStack.last(), packedLight, 65, 69, 64, 255);
          } else {
            this.slatArmour.render(vertexConsumer, poseStack.last(), packedLight);
          }
        } else if (this.shouldRenderSlatBlueprint(tank)) {
          this.slatArmour.render(buffer.getBuffer(RenderType.entityTranslucent(tankTexture)),
              poseStack.last(), packedLight, 70, 205, 255, 105);
        }
      }
      poseStack.popPose();
    }
    poseStack.popPose();
    super.render(tank, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private void renderYawHatch(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, T72B3ObjModel hatch,
      double pivotX, double pivotY, double pivotZ, float angle) {
    poseStack.pushPose();
    poseStack.translate(pivotX, pivotY, pivotZ);
    poseStack.mulPose(Axis.YP.rotationDegrees(angle));
    poseStack.translate(-pivotX, -pivotY, -pivotZ);
    hatch.render(consumer, poseStack.last(), light);
    poseStack.popPose();
  }

  private void renderShellMechanism(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float progress) {
    poseStack.pushPose();
    poseStack.translate(0.1753D, 0.84815D, -1.1295D);
    poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F * progress));
    poseStack.mulPose(Axis.YP.rotationDegrees(-17.5F * progress));
    poseStack.translate(-0.1753D, -0.84815D, 1.1295D);
    this.shellMechanism.render(consumer, poseStack.last(), light);
    poseStack.popPose();
  }

  private void renderElevatingGun(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, float pitch, float recoil,
      boolean blueprint, boolean destroyed) {
    poseStack.pushPose();
    poseStack.translate(0.0D, 0.3053D, 1.471D);
    poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    poseStack.translate(0.0D, -0.3053D, -1.471D);
    if (blueprint) {
      this.mainGun.render(consumer, poseStack.last(), light, 70, 205, 255, 105);
    } else if (destroyed) {
      this.mainGun.render(consumer, poseStack.last(), light, 70, 74, 68, 255);
    } else {
      this.mainGun.render(consumer, poseStack.last(), light);
    }
    poseStack.translate(0.0D, 0.0D, -0.22D * recoil);
    if (blueprint) {
      this.gunTube.render(consumer, poseStack.last(), light, 70, 205, 255, 105);
    } else if (destroyed) {
      this.gunTube.render(consumer, poseStack.last(), light, 62, 66, 61, 255);
    } else {
      this.gunTube.render(consumer, poseStack.last(), light);
    }
    poseStack.popPose();
  }

  private void renderHatch(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int packedLight,
      T72B3ObjModel hatch, double pivotX, double pivotY, double pivotZ, float angle,
      boolean destroyed) {
    poseStack.pushPose();
    poseStack.translate(pivotX, pivotY, pivotZ);
    poseStack.mulPose(Axis.XP.rotationDegrees(angle));
    poseStack.translate(-pivotX, -pivotY, -pivotZ);
    if (destroyed) {
      hatch.render(vertexConsumer, poseStack.last(), packedLight, 68, 72, 66, 255);
    } else {
      hatch.render(vertexConsumer, poseStack.last(), packedLight);
    }
    poseStack.popPose();
  }

  private void renderCommanderHatchAssembly(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int packedLight,
      float angle, boolean destroyed) {
    poseStack.pushPose();
    // Hinge the complete panel at the rear rim and lift its forward edge. This keeps the upright
    // lid behind the occupant and away from the forward-facing coaxial machine gun.
    poseStack.translate(-0.7415D, 1.01D, -0.883991D);
    poseStack.mulPose(Axis.XP.rotationDegrees(angle));
    poseStack.translate(0.7415D, -1.01D, 0.883991D);
    if (destroyed) {
      this.commanderCupola.render(vertexConsumer, poseStack.last(), packedLight,
          68, 72, 66, 255);
      this.commanderHatch.render(vertexConsumer, poseStack.last(), packedLight,
          68, 72, 66, 255);
    } else {
      this.commanderCupola.render(vertexConsumer, poseStack.last(), packedLight);
      this.commanderHatch.render(vertexConsumer, poseStack.last(), packedLight);
    }
    poseStack.popPose();
  }

  private void renderWheelSet(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int packedLight,
      T72B3ObjModel[] wheels, double[][] pivots, float rotation) {
    for (int index = 0; index < wheels.length; index++) {
      poseStack.pushPose();
      poseStack.translate(pivots[index][0], pivots[index][1], pivots[index][2]);
      poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
      poseStack.translate(-pivots[index][0], -pivots[index][1], -pivots[index][2]);
      wheels[index].render(vertexConsumer, poseStack.last(), packedLight);
      poseStack.popPose();
    }
  }

  /** Recreates MTS's generated tread belt by repeating the original track_s link around a loop. */
  private void renderTread(PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int packedLight,
      double sideX, float wheelRotation, boolean blueprint) {
    double totalLength = 0.0D;
    for (int index = 0; index < TREAD_LOOP.length; index++) {
      double[] from = TREAD_LOOP[index];
      double[] to = TREAD_LOOP[(index + 1) % TREAD_LOOP.length];
      totalLength += Math.hypot(to[0] - from[0], to[1] - from[1]);
    }
    int linkCount = Math.max(1, Mth.floor(totalLength / TREAD_SPACING));
    double step = totalLength / linkCount;
    double phase = wheelRotation / 360.0D * Math.PI;
    phase %= step;
    if (phase < 0.0D) phase += step;
    for (int link = 0; link < linkCount; link++) {
      double distance = (link * step + phase) % totalLength;
      double traversed = 0.0D;
      for (int segment = 0; segment < TREAD_LOOP.length; segment++) {
        double[] from = TREAD_LOOP[segment];
        double[] to = TREAD_LOOP[(segment + 1) % TREAD_LOOP.length];
        double dy = to[0] - from[0];
        double dz = to[1] - from[1];
        double segmentLength = Math.hypot(dy, dz);
        if (distance <= traversed + segmentLength || segment == TREAD_LOOP.length - 1) {
          double progress = Mth.clamp((distance - traversed) / segmentLength, 0.0D, 1.0D);
          double y = Mth.lerp(progress, from[0], to[0]);
          double z = Mth.lerp(progress, from[1], to[1]);
          float angle = (float) Math.toDegrees(Math.atan2(-dy, dz));
          poseStack.pushPose();
          poseStack.translate(sideX, y, z);
          poseStack.mulPose(Axis.XP.rotationDegrees(angle));
          if (blueprint) {
            this.treadLink.render(vertexConsumer, poseStack.last(), packedLight,
                70, 205, 255, 105);
          } else {
            this.treadLink.render(vertexConsumer, poseStack.last(), packedLight);
          }
          poseStack.popPose();
          break;
        }
        traversed += segmentLength;
      }
    }
  }

  private boolean shouldRenderTrackBlueprint(T72B3Entity tank) {
    var player = Minecraft.getInstance().player;
    if (player == null || player.distanceToSqr(tank) > 144.0D) return false;
    return player.getMainHandItem().is(VehicleContent.TRACK_S.get())
        || player.getOffhandItem().is(VehicleContent.TRACK_S.get());
  }

  private boolean shouldRenderTurretBlueprint(T72B3Entity tank) {
    var player = Minecraft.getInstance().player;
    return player != null && player.distanceToSqr(tank) <= 144.0D
        && (player.getMainHandItem().is(VehicleContent.T72B3_TURRET.get())
            || player.getOffhandItem().is(VehicleContent.T72B3_TURRET.get()));
  }

  private boolean shouldRenderSlatBlueprint(T72B3Entity tank) {
    var player = Minecraft.getInstance().player;
    return player != null && player.distanceToSqr(tank) <= 144.0D
        && (player.getMainHandItem().is(VehicleContent.RESHETKA_EPTA.get())
            || player.getOffhandItem().is(VehicleContent.RESHETKA_EPTA.get()));
  }

  private static String[] staticBodyExclusions() {
    String[] exclusions = new String[25];
    exclusions[0] = "dlyadebilovsuka";
    for (int index = 0; index < 11; index++) {
      exclusions[1 + index] = "lw" + index;
      exclusions[12 + index] = "rw" + index;
    }
    exclusions[23] = "&headlights";
    exclusions[24] = "door_d";
    return exclusions;
  }

  /**
   * Render-frame interpolation matching the original vehicle renderer. Wheel travel is derived
   * from the already-interpolated visible chassis position, so it remains smooth above 20 FPS and
   * cannot visibly jump when an entity-data packet arrives.
   */
  private static final class VehicleVisualState {
    private boolean initialized;
    private Vec3 previousPosition = Vec3.ZERO;
    private float previousYaw;
    private float leftWheelRotation;
    private float rightWheelRotation;
    private float turretYaw;
    private float turretPitch;
    private float recoil;
    private float commanderHatchProgress;
    private float shooterHatchProgress;
    private float commanderHatchAngle;
    private float shooterHatchAngle;
    private float driverHatchProgress;
    private float driverHatchAngle;
    private float loaderHatchProgress;
    private float loaderHatchAngle;
    private float reloadDip;
    private float bodyPitch;
    private float bodyRoll;
    private float suspensionOffset;
    private long previousFrameNanos;

    private void update(T72B3Entity tank, float renderedYaw, float partialTick) {
      Vec3 renderedPosition = tank.getPosition(partialTick);
      long now = System.nanoTime();
      if (!this.initialized) {
        this.initialized = true;
        this.previousPosition = renderedPosition;
        this.previousYaw = renderedYaw;
        this.leftWheelRotation = tank.getLeftTrackRotation();
        this.rightWheelRotation = tank.getRightTrackRotation();
        this.turretYaw = tank.getTurretYaw();
        this.turretPitch = tank.getTurretPitch();
        this.recoil = tank.getRecoil();
        this.commanderHatchProgress = tank.isLeftRearSeatExposed() ? 1.0F : 0.0F;
        this.shooterHatchProgress = tank.isRightRearSeatExposed() ? 1.0F : 0.0F;
        this.commanderHatchAngle = -92.5F * smoothStep(this.commanderHatchProgress);
        this.shooterHatchAngle = 92.5F * smoothStep(this.shooterHatchProgress);
        this.driverHatchProgress = tank.isDriverSeatOccupied() ? 1.0F : 0.0F;
        this.driverHatchAngle = -125.0F * smoothStep(this.driverHatchProgress);
        this.bodyPitch = tank.getBodyPitch();
        this.bodyRoll = tank.getBodyRoll();
        this.suspensionOffset = tank.getSuspensionOffset();
        this.previousFrameNanos = now;
        return;
      }

      double dx = renderedPosition.x - this.previousPosition.x;
      double dz = renderedPosition.z - this.previousPosition.z;
      double distanceSqr = dx * dx + dz * dz;
      float yawDeltaDegrees = Mth.wrapDegrees(renderedYaw - this.previousYaw);
      if (distanceSqr > 16.0D) {
        // Teleport/dimension correction: establish a new baseline instead of spinning the tracks.
        this.leftWheelRotation = tank.getLeftTrackRotation();
        this.rightWheelRotation = tank.getRightTrackRotation();
      } else {
        float yawRadians = renderedYaw * Mth.DEG_TO_RAD;
        double forwardTravel = dx * -Mth.sin(yawRadians) + dz * Mth.cos(yawRadians);
        double differentialTravel = yawDeltaDegrees * Mth.DEG_TO_RAD * 2.40D * 0.78D;
        this.leftWheelRotation = Mth.wrapDegrees(this.leftWheelRotation
            - (float) ((forwardTravel - differentialTravel) * 83.0D));
        this.rightWheelRotation = Mth.wrapDegrees(this.rightWheelRotation
            - (float) ((forwardTravel + differentialTravel) * 83.0D));
      }

      double frameSeconds = Mth.clamp((now - this.previousFrameNanos) / 1_000_000_000.0D,
          0.0D, 0.10D);
      float turretBlend = (float) (1.0D - Math.exp(-20.0D * frameSeconds));
      this.turretYaw = Mth.wrapDegrees(this.turretYaw
          + Mth.wrapDegrees(tank.getTurretYaw() - this.turretYaw) * turretBlend);
      this.turretPitch += (tank.getTurretPitch() - this.turretPitch) * turretBlend;
      this.recoil += (tank.getRecoil() - this.recoil) * turretBlend;
      float groundBlend = (float) (1.0D - Math.exp(-11.0D * frameSeconds));
      this.bodyPitch += Mth.wrapDegrees(tank.getBodyPitch() - this.bodyPitch) * groundBlend;
      this.bodyRoll += Mth.wrapDegrees(tank.getBodyRoll() - this.bodyRoll) * groundBlend;
      this.suspensionOffset += (tank.getSuspensionOffset() - this.suspensionOffset) * groundBlend;
      float hatchAdvance = (float) (frameSeconds / 0.55D);
      this.commanderHatchProgress = approach01(this.commanderHatchProgress,
          tank.isLeftRearSeatExposed(), hatchAdvance);
      this.shooterHatchProgress = approach01(this.shooterHatchProgress,
          tank.isRightRearSeatExposed(), hatchAdvance);
      this.commanderHatchAngle = -92.5F * smoothStep(this.commanderHatchProgress);
      this.shooterHatchAngle = 92.5F * smoothStep(this.shooterHatchProgress);
      this.driverHatchProgress = approach01(this.driverHatchProgress,
          tank.isDriverSeatOccupied(), hatchAdvance);
      this.driverHatchAngle = -125.0F * smoothStep(this.driverHatchProgress);
      float reloadPhase = tank.getReloadTicks() <= 0 ? 0.0F
          : 1.0F - tank.getReloadTicks() / 140.0F;
      float reloadOpen = reloadPhase <= 0.0F ? 0.0F
          : Mth.sin(Mth.PI * Mth.clamp(reloadPhase, 0.0F, 1.0F));
      this.loaderHatchProgress += (reloadOpen - this.loaderHatchProgress) * turretBlend;
      this.loaderHatchAngle = -100.0F * smoothStep(this.loaderHatchProgress);
      this.reloadDip = -5.0F * this.loaderHatchProgress;
      this.previousPosition = renderedPosition;
      this.previousYaw = renderedYaw;
      this.previousFrameNanos = now;
    }

    private static float approach01(float value, boolean open, float amount) {
      return Mth.clamp(value + (open ? amount : -amount), 0.0F, 1.0F);
    }

    private static float smoothStep(float value) {
      return value * value * (3.0F - 2.0F * value);
    }
  }

  @Override
  public ResourceLocation getTextureLocation(T72B3Entity tank) {
    return TEXTURES[Mth.clamp(tank.getSkin(), 0, TEXTURES.length - 1)];
  }
}
