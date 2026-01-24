package me.dw1e.ff.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.dw1e.ff.FairFight;
import me.dw1e.ff.check.Check;
import me.dw1e.ff.check.CheckManager;
import me.dw1e.ff.config.ConfigValue;
import me.dw1e.ff.data.processor.EmulationProcessor;
import me.dw1e.ff.misc.collision.BoundingBox;
import me.dw1e.ff.misc.collision.Cuboid;
import me.dw1e.ff.misc.collision.HitboxEntity;
import me.dw1e.ff.misc.evicting.EvictingList;
import me.dw1e.ff.misc.math.MathUtil;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.misc.util.PlayerUtil;
import me.dw1e.ff.misc.util.ServerUtil;
import me.dw1e.ff.misc.util.StringUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.*;
import me.dw1e.ff.packet.wrapper.server.*;
import me.dw1e.ff.util.ViaVersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerData {

    private final Player player;
    private final List<Check> checks;
    private final Location location;
    private final EvictingList<Location> teleports = new EvictingList<>(15);
    private final Map<Short, Long> transactionMap = new HashMap<>();
    private final Map<Short, Runnable> actionMap = new HashMap<>();
    private final Map<Integer, HitboxEntity> entityMap = new HashMap<>();
    private final Map<BlockPosition, Boolean> ghostBlocks = new HashMap<>();
    private final EmulationProcessor emulationProcessor = new EmulationProcessor(this);

    private Location lastLastLocation, lastLocation;

    private boolean alerts, verbose, bypass, kicked, punished; // 玩家在反作弊内的一些设置

    private boolean // 玩家行为/状态
            updatePos, allowedFly, isFlying, instantlyBuild,
            lastLastClientGround, lastClientGround, clientGround, mathGround,
            jumped, inVehicle, sprinting, sneaking, placing, blocking, eating, drawingBow, inventoryOpen,
            digging, abortedDigging, stoppedDigging;

    private int itemSlot, usingItemSlot; // 物品栏位
    private boolean inSlotChanging;

    private boolean // 上一次的状态
            wasOnSlime, wasInWeb, wasInWater, wasInLava, wasInFlowingWater, wasInFlowingLava;

    private boolean // 碰撞
            climbing, nearVehicle, nearBoat,
            inWater, inLava, inFlowingWater, inFlowingLava,
            underBlock,
            inWeb,
            nearStep, pushedByPiston,
            serverGround, onSlime,
            nearWall;

    private float walkSpeed, friction, lastFriction;

    private float // 视角类
            lastLastDeltaYaw, lastLastDeltaPitch,
            lastDeltaYaw, lastDeltaPitch,
            deltaYaw, deltaPitch,
            accelYaw, accelPitch;

    private double // 位置类
            lastLastDeltaY, lastDeltaX, lastDeltaY, lastDeltaZ, lastDeltaXZ, deltaX, deltaY, deltaZ, deltaXZ,
            velocityX, velocityY, velocityZ, velocityXZ;

    private int jumpEffect, speedEffect, slowEffect; // 药水效果

    private int // 玩家行为类的计时
            tick,
            tickSinceTeleport = 20,
            tickSinceAttack = 20,
            tickSinceVelocity = 20,
            tickSinceOtherVelocity = 20,
            maxVelocityTicks,
            tickSinceAbilityChange = 20,
            tickSinceClientGround = 20,
            tickSinceSprinting = 20,
            tickSinceJumped = 20,
            tickSinceUsingItem = 20,
            tickSinceDroppedItem = 20,
            tickSinceOffsetMotion = 20,
            tickSinceSteerVehicle = 20,
            tickSinceRidingInteract = 20,
            tickSincePlacedBlock = 20,
            inventoryOpenTicks;

    private int // 碰撞类的计时
            climbingTicks = 20,
            tickSinceClimbing = 20,
            tickSinceInLiquid = 20,
            tickSinceInFlowingLava = 20,
            liquidTicks,
            tickSinceUnderBlock = 20,
            tickSinceInWeb = 20,
            tickSinceNearStep = 20,
            tickSincePushedByPiston = 20,
            tickSinceOnSlime = 20,
            tickSinceNearWall = 20,
            tickSinceServerGround = 20;

    private short transId = Short.MIN_VALUE;

    private long transPing, lastSentTransaction, lastRepliedTransaction;

    private long lastFlyingTime, flyingTime; // 此处滞后判断不可信, 仅用于防止fakeLag与blink类作弊的过于离谱

    private Player lastTarget;

    public PlayerData(Player player) {
        this.player = player;

        // 初始化一些数据

        lastLastLocation = lastLocation = location = player.getLocation().clone();

        speedEffect = PlayerUtil.getAmplifier(player, PotionEffectType.SPEED);
        slowEffect = PlayerUtil.getAmplifier(player, PotionEffectType.SLOW);
        jumpEffect = PlayerUtil.getAmplifier(player, PotionEffectType.JUMP);

        itemSlot = player.getInventory().getHeldItemSlot();

        walkSpeed = player.getWalkSpeed() / 2.0F;
        allowedFly = player.getAllowFlight();
        isFlying = player.isFlying();
        instantlyBuild = player.getGameMode().equals(GameMode.CREATIVE);

        inVehicle = player.isInsideVehicle();

        sprinting = player.isSprinting();
        sneaking = player.isSneaking();

        lastSentTransaction = lastRepliedTransaction = lastFlyingTime = flyingTime = System.currentTimeMillis();

        FairFight.INSTANCE.sendToMainThread(() ->
                player.getWorld().getEntities().stream()
                        .filter(entity -> entity instanceof Player)
                        .forEach(entity -> {
                            Location targetLoc = entity.getLocation().clone();

                            int x = (int) (targetLoc.getX() * 32.0);
                            int y = (int) (targetLoc.getY() * 32.0);
                            int z = (int) (targetLoc.getZ() * 32.0);

                            addEntityToMap(entity.getEntityId(), x, y, z);
                        })
        );

        checks = FairFight.INSTANCE.getCheckManager().loadChecks(this);

        alerts = player.hasMetadata("FAIR_FIGHT_ALERTS") && player.hasPermission("fairfight.command.alerts");
        verbose = player.hasMetadata("FAIR_FIGHT_VERBOSE") && player.hasPermission("fairfight.command.verbose");
        bypass = player.hasPermission("fairfight.bypass");

        if (ConfigValue.ignore_high_version && FairFight.isViaVersionEnabled) {
            if (ViaVersionUtil.isPlayerHighVersion(player)) {
                bypass = true;
            }
        }
    }

    public void process(WrappedPacket packet) {
        preProcess(packet);

        emulationProcessor.process(packet);

        CheckManager checkManager = FairFight.INSTANCE.getCheckManager();

        if (!bypass) {
            for (Check check : new ArrayList<>(checks)) {
                if (!checkManager.getCheckValue(check).isEnabled()) continue;

                check.handle(packet);
            }
        }

        postProcess(packet);
    }

    private void preProcess(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            CPacketFlying wrapper = (CPacketFlying) packet;

            lastFlyingTime = flyingTime;
            flyingTime = packet.getTimestamp();

            updatePos = wrapper.isPosition();

            lastLastClientGround = lastClientGround;
            lastClientGround = clientGround;
            clientGround = wrapper.isOnGround();

            mathGround = wrapper.getY() % 0.015625 == 0.0;

            lastLastLocation = lastLocation.clone();
            lastLocation = location.clone();

            if (wrapper.isPosition()) {
                location.setX(wrapper.getX());
                location.setY(wrapper.getY());
                location.setZ(wrapper.getZ());
            }

            if (wrapper.isRotation()) {
                location.setYaw(wrapper.getYaw());
                location.setPitch(wrapper.getPitch());
            }

            if (wrapper.isPosition() && wrapper.isRotation()) {
                for (Location tpLoc : teleports) {
                    if (!wrapper.isOnGround() // 增加0.03的冗余, 在卡方块里/离开坐骑时传送的位置略微有出入
                            && Math.abs(tpLoc.getX() - wrapper.getX()) <= 0.03
                            && Math.abs(tpLoc.getY() - wrapper.getY()) <= 0.03
                            && Math.abs(tpLoc.getZ() - wrapper.getZ()) <= 0.03) {
                        tickSinceTeleport = 0;

                        // !!! 不要给所有移动检测都增加传送豁免 !!!
                        // 一个新的Matrix Fly绕过已经发现, 且截至到 2026/1/14 日暂未修复
                        // 很多反作弊也可使用, 例如Vulcan, Verus等
                        // 原理: 利用原版Spigot的穿墙拉回(xxx moved wrongly)实现被传送而后获得传送豁免

                        lastLocation = tpLoc.clone(); // 将上一次的位置设置为传送目的地, 让一些检测不使用传送豁免以防绕过
                        clientGround = mathGround && serverGround; // 传送时地面状态永远为否, 所以需要此处修正

                        teleports.remove(tpLoc);
                        break;
                    }
                }
            }

            lastLastDeltaY = lastDeltaY;

            lastDeltaX = deltaX;
            lastDeltaY = deltaY;
            lastDeltaZ = deltaZ;
            lastDeltaXZ = deltaXZ;

            deltaX = location.getX() - lastLocation.getX();
            deltaY = location.getY() - lastLocation.getY();
            deltaZ = location.getZ() - lastLocation.getZ();
            deltaXZ = MathUtil.hypot(Math.abs(deltaX), Math.abs(deltaZ));

            lastLastDeltaYaw = lastDeltaYaw;
            lastLastDeltaPitch = lastDeltaPitch;

            lastDeltaYaw = deltaYaw;
            lastDeltaPitch = deltaPitch;

            deltaYaw = Math.abs(location.getYaw() - lastLocation.getYaw()) % 360.0F; // 在重启时可能会出现一些问题
            deltaPitch = Math.abs(location.getPitch() - lastLocation.getPitch());

            accelYaw = Math.abs(deltaYaw - lastDeltaYaw);
            accelPitch = Math.abs(deltaPitch - lastDeltaPitch);

            if (lastDeltaXZ > 0.0 && deltaXZ == 0.0 && !wrapper.isPosition()) tickSinceOffsetMotion = 0;

            jumped = testJumped();

            handleCollisions();
            processTicks();
        } else if (packet instanceof CPacketAbilities) {
            CPacketAbilities wrapper = (CPacketAbilities) packet;

            if (allowedFly) { // 原版服务器好像就自带一个判断, 不过保险起见我选择加一个自己的
                if (isFlying != wrapper.isFlying()) tickSinceAbilityChange = 0;

                isFlying = wrapper.isFlying();
            }
        } else if (packet instanceof CPacketBlockDig) {
            CPacketBlockDig wrapper = (CPacketBlockDig) packet;

            switch (wrapper.getPlayerDigType()) {
                case START_DESTROY_BLOCK:
                    digging = true;
                    abortedDigging = false;
                    stoppedDigging = false;
                    break;

                case ABORT_DESTROY_BLOCK:
                case STOP_DESTROY_BLOCK:
                    digging = false;
                    break;

                case DROP_ITEM:
                case DROP_ALL_ITEMS:
                    tickSinceDroppedItem = 0;
                case RELEASE_USE_ITEM:
                    blocking = eating = drawingBow = false;
                    break;
            }

            if (instantlyBuild) digging = false; // 创造模式瞬间破坏方块, 不会发送中断/停止dig
        } else if (packet instanceof CPacketBlockPlace) {
            CPacketBlockPlace wrapper = ((CPacketBlockPlace) packet);

            placing = true;

            if (wrapper.isUseItem()) {
                Material material = wrapper.getItemStack().getType();

                if (canBlock(material)) blocking = true;
                if (canEat(wrapper.getItemStack())) eating = true;
                if (canDrawBow(material)) drawingBow = true;

                if (blocking || eating || drawingBow) usingItemSlot = itemSlot;
            }

            if (wrapper.isPlacedBlock()) {
                tickSincePlacedBlock = 0;

                ghostBlocks.put(wrapper.getBlockPosition(), false); // 布尔值为: 是否于下一flying tick中移除

                transConfirm(() -> ghostBlocks.put(wrapper.getBlockPosition(), true)); // 与服务器状态同步(例如: 放置被取消)
            }
        } else if (packet instanceof CPacketClientCommand) {
            CPacketClientCommand wrapper = (CPacketClientCommand) packet;

            if (wrapper.getClientCommand() == EnumWrappers.ClientCommand.OPEN_INVENTORY_ACHIEVEMENT) {
                inventoryOpen = true;
            }
        } else if (packet instanceof CPacketCloseWindow) {
            inventoryOpen = false;

        } else if (packet instanceof CPacketEntityAction) {
            CPacketEntityAction wrapper = (CPacketEntityAction) packet;

            switch (wrapper.getAction()) {
                case START_SNEAKING:
                    sneaking = true;
                    break;
                case STOP_SNEAKING:
                    sneaking = false;
                    break;
                case START_SPRINTING:
                    sprinting = true;
                    break;
                case STOP_SPRINTING:
                    sprinting = false;
                    break;
            }
        } else if (packet instanceof CPacketHeldItemSlot) {
            CPacketHeldItemSlot wrapper = (CPacketHeldItemSlot) packet;

            itemSlot = wrapper.getSlot();

            if (usingItemSlot != itemSlot) blocking = eating = drawingBow = false;
        } else if (packet instanceof CPacketTransaction) {
            CPacketTransaction wrapper = (CPacketTransaction) packet;

            short id = wrapper.getActionId();
            long time = wrapper.getTimestamp();

            if (transactionMap.containsKey(id)) {
                transPing = time - transactionMap.remove(id);
                lastRepliedTransaction = time;

                Runnable action = actionMap.remove(id);
                if (action != null) action.run();
            }
        } else if (packet instanceof CPacketUseEntity) {
            CPacketUseEntity wrapper = (CPacketUseEntity) packet;

            switch (wrapper.getAction()) {
                case ATTACK:
                    tickSinceAttack = 0;
                    lastTarget = ServerUtil.getPlayerByEntityId(wrapper.getEntityId());

                    break;
                case INTERACT: {
                    Entity entity = ServerUtil.getEntityByEntityId(player.getWorld(), wrapper.getEntityId());

                    // 与可骑乘生物交互, 在空中右键坐骑乘坐时传送位置会有较大出入, 使用此方法豁免一些检查
                    if (entity instanceof Vehicle) tickSinceRidingInteract = 0;

                    break;
                }
            }
        } else if (packet instanceof CPacketWindowClick) {
            inventoryOpen = true;

        } else if (packet instanceof SPacketAbilities) {
            SPacketAbilities wrapper = (SPacketAbilities) packet;

            transConfirm(() -> {
                if (allowedFly != wrapper.isAllowedFly() || isFlying != wrapper.isFlying()) tickSinceAbilityChange = 0;

                allowedFly = wrapper.isAllowedFly();
                isFlying = wrapper.isFlying();
                instantlyBuild = wrapper.isInstantlyBuild();

                walkSpeed = wrapper.getWalkSpeed();
            });
        } else if (packet instanceof SPacketCloseWindow) {
            transConfirm(() -> inventoryOpen = false);

        } else if (packet instanceof SPacketEntity) {
            SPacketEntity wrapper = (SPacketEntity) packet;

            HitboxEntity entity = entityMap.get(wrapper.getEntityId());

            if (entity == null) return;

            transConfirm(() -> {
                entity.serverPosX += wrapper.getX();
                entity.serverPosY += wrapper.getY();
                entity.serverPosZ += wrapper.getZ();

                double dx = (double) entity.serverPosX / 32.0;
                double dy = (double) entity.serverPosY / 32.0;
                double dz = (double) entity.serverPosZ / 32.0;

                entity.setPosition2(dx, dy, dz);
            });
        } else if (packet instanceof SPacketEntityDestroy) {
            SPacketEntityDestroy wrapper = (SPacketEntityDestroy) packet;

            for (int id : wrapper.getEntities()) entityMap.remove(id);
        } else if (packet instanceof SPacketEntityEffect) {
            SPacketEntityEffect wrapper = (SPacketEntityEffect) packet;

            if (wrapper.getEntityId() == player.getEntityId()) {
                int effectId = wrapper.getEffectId(), amplifier = wrapper.getAmplifier() + 1;

                transConfirm(() -> {
                    if (effectId == 1) speedEffect = amplifier;
                    else if (effectId == 2) slowEffect = amplifier;
                    else if (effectId == 8) jumpEffect = amplifier;
                });
            }
        } else if (packet instanceof SPacketEntityTeleport) {
            SPacketEntityTeleport wrapper = (SPacketEntityTeleport) packet;

            HitboxEntity entity = entityMap.get(wrapper.getEntityId());

            if (entity == null) return;

            transConfirm(() -> {
                entity.serverPosX = wrapper.getX();
                entity.serverPosY = wrapper.getY();
                entity.serverPosZ = wrapper.getZ();

                double dx = (double) entity.serverPosX / 32.0;
                double dy = (double) entity.serverPosY / 32.0;
                double dz = (double) entity.serverPosZ / 32.0;

                if (Math.abs(entity.posX - dx) < 0.03125
                        && Math.abs(entity.posY - dy) < 0.015625
                        && Math.abs(entity.posZ - dz) < 0.03125) {
                    entity.setPosition2(entity.posX, entity.posY, entity.posZ);

                } else entity.setPosition2(dx, dy, dz);
            });
        } else if (packet instanceof SPacketEntityVelocity) {
            SPacketEntityVelocity wrapper = (SPacketEntityVelocity) packet;

            if (wrapper.getEntityId() != player.getEntityId()) return;

            transConfirm(() -> {
                if (wrapper.getY() >= 0.0) {
                    tickSinceVelocity = 0;
                } else { // y < 0 一般为摔伤
                    tickSinceOtherVelocity = 0;
                }

                velocityX = wrapper.getX();
                velocityY = wrapper.getY();
                velocityZ = wrapper.getZ();

                velocityXZ = MathUtil.hypot(Math.abs(velocityX), Math.abs(velocityZ));

                // 击退滞空时长只受垂直击退影响, 在相同垂直高度的情况下, 水平击退为0.1与2.0的击退滞空时长均相同
                // 但是有可能会遇到y_limit的情况, 所以最少给8ticks
                maxVelocityTicks = (int) Math.max(Math.round(20.0 * Math.pow(velocityY, 0.6)), 8);
            });
        } else if (packet instanceof SPacketNamedEntitySpawn) {
            SPacketNamedEntitySpawn wrapper = (SPacketNamedEntitySpawn) packet;

            if (wrapper.getEntityId() == player.getEntityId()) return;

            addEntityToMap(wrapper.getEntityId(), wrapper.getX(), wrapper.getY(), wrapper.getZ());
        } else if (packet instanceof SPacketOpenWindow) {
            transConfirm(() -> {
                inventoryOpen = true;

                digging = false;

                blocking = eating = drawingBow = false;
            });
        } else if (packet instanceof SPacketPosition) {
            SPacketPosition wrapper = (SPacketPosition) packet;

            Location teleportLocation = new Location(player.getWorld(),
                    wrapper.getX(), wrapper.getY(), wrapper.getZ(),
                    wrapper.getYaw(), wrapper.getPitch());

            transConfirm(() -> teleports.add(teleportLocation));
        } else if (packet instanceof SPacketRemoveEntityEffect) {
            SPacketRemoveEntityEffect wrapper = (SPacketRemoveEntityEffect) packet;

            if (wrapper.getEntityId() == player.getEntityId()) {
                int effectId = wrapper.getEffectId();

                transConfirm(() -> {
                    if (effectId == 1) speedEffect = 0;
                    else if (effectId == 2) slowEffect = 0;
                    else if (effectId == 8) jumpEffect = 0;
                });
            }
        } else if (packet instanceof SPacketTransaction) {
            SPacketTransaction wrapper = (SPacketTransaction) packet;

            transactionMap.put(wrapper.getActionId(), wrapper.getTimestamp());
        }
    }

    private void postProcess(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            entityMap.values().forEach(HitboxEntity::onLivingUpdate);

            velocityX = velocityZ = velocityXZ = 0.0;

            velocityY = Math.max(0.0, (velocityY - 0.08) * 0.98F); // 给其它检测一个简单的预测

            wasOnSlime = onSlime;
            wasInWeb = inWeb;
            wasInWater = inWater;
            wasInLava = inLava;
            wasInFlowingWater = inFlowingWater;
            wasInFlowingLava = inFlowingLava;

            placing = false;
        }
    }

    public void reset() {
        sprinting = sneaking = blocking = eating = drawingBow = inventoryOpen = false;
    }

    private void addEntityToMap(int entityId, int x, int y, int z) {
        if (!entityMap.containsKey(entityId)) Bukkit.getScheduler().runTaskLater(FairFight.INSTANCE.getPlugin(), () ->
                entityMap.put(entityId, new HitboxEntity(x, y, z)), 3L);
    }

    private void processTicks() {
        ++tick;
        ++tickSinceTeleport;
        ++tickSinceAttack;
        ++tickSinceVelocity;
        ++tickSinceOtherVelocity;
        ++tickSinceOffsetMotion;
        ++tickSinceAbilityChange;
        ++tickSinceRidingInteract;
        ++tickSincePlacedBlock;
        ++tickSinceDroppedItem;

        tickSinceClientGround = clientGround ? 0 : ++tickSinceClientGround;
        tickSinceSprinting = sprinting ? 0 : ++tickSinceSprinting;
        tickSinceJumped = jumped ? 0 : ++tickSinceJumped;
        tickSinceUsingItem = isUsingItem() ? 0 : ++tickSinceUsingItem;
        tickSinceSteerVehicle = inVehicle ? 0 : ++tickSinceSteerVehicle;
        inventoryOpenTicks = inventoryOpen ? ++inventoryOpenTicks : 0;

        climbingTicks = climbing ? ++climbingTicks : 0;
        tickSinceClimbing = climbing ? 0 : ++tickSinceClimbing;
        tickSinceInLiquid = isInLiquid() ? 0 : ++tickSinceInLiquid;
        tickSinceInFlowingLava = wasInFlowingLava ? 0 : ++tickSinceInFlowingLava;
        liquidTicks = isInLiquid() ? ++liquidTicks : 0;
        tickSinceInWeb = wasInWeb ? 0 : ++tickSinceInWeb;
        tickSinceNearStep = nearStep ? 0 : ++tickSinceNearStep;
        tickSincePushedByPiston = pushedByPiston ? 0 : ++tickSincePushedByPiston;
        tickSinceOnSlime = wasOnSlime ? 0 : ++tickSinceOnSlime;
        tickSinceUnderBlock = underBlock ? 0 : ++tickSinceUnderBlock;
        tickSinceNearWall = nearWall ? 0 : ++tickSinceNearWall;
        tickSinceServerGround = serverGround ? 0 : ++tickSinceServerGround;

        ghostBlocks.entrySet().removeIf(Map.Entry::getValue);

        ItemStack slotItem = player.getInventory().getItem(itemSlot);

        if (slotItem != null) {
            Material material = slotItem.getType();

            if (blocking && !canBlock(material)) blocking = false;
            if (eating && !canEat(slotItem)) eating = false;
            if (drawingBow && !canDrawBow(Material.BOW)) drawingBow = false;
        }

        if (abortedDigging) {
            abortedDigging = false;
            digging = false;
        }

        if (stoppedDigging) {
            stoppedDigging = false;
            digging = false;
        }
    }

    private void handleCuboid() {
        // 玩家内部
        Cuboid inPlayer = new Cuboid(location).expandXZ(0.29901).expandY(0.0, 1.8F);

        // 玩家脚下
        Cuboid under = new Cuboid(location).expandXZ(0.3F).expandY(0.000001, 0.0);

        // 玩家头上
        Cuboid above = new Cuboid(location)
                .expandX(0.3F + Math.abs(deltaX)).expandZ(0.3F + Math.abs(deltaZ)) // 加上可抵达的距离
                .expandY(0.0, 1.8F + Math.max(getAttributeJump(), velocityY)); // 加上可抵达的高度

        // 玩家周围
        Cuboid near = new Cuboid(location).expandXZ(0.300001).expandY(0.0, 1.8F);

        // 液体
        Cuboid water = new Cuboid(location).expandXZ(0.2990001).moveY(0.401, 1.399); // 水碰撞箱, 消耗氧气≠受到了水中摩擦
        Cuboid lava = new Cuboid(location).expandXZ(0.2990001).moveY(0.115, 1.4); // 岩浆碰撞箱, 与水同理

        // 栅栏, 栅栏门, 石墙的碰撞会高0.5格(为1.5格), 但方块体积依然为1.0格高
        Cuboid fence = new Cuboid(location).expandXZ(0.3F).expandY(0.500001, 0.0);

        // --------------------------------------------------
        // 液体
        inWater = inLava = inFlowingWater = inFlowingLava = false;

        for (Block block : water.getBlocks()) {
            Material material = block.getType();
            boolean isWater = material.equals(Material.WATER) || material.equals(Material.STATIONARY_WATER);

            inWater |= isWater;
            inFlowingWater |= isWater && BlockUtil.getBlockData(block) != 0;
        }

        for (Block block : lava.getBlocks()) {
            Material material = block.getType();
            boolean isLava = material.equals(Material.LAVA) || material.equals(Material.STATIONARY_LAVA);

            inLava |= isLava;
            inFlowingLava |= isLava && BlockUtil.getBlockData(block) != 0;
        }

        // --------------------------------------------------
        // 玩家内部
        inWeb = false;

        for (Block block : inPlayer.getBlocks()) {
            Material material = block.getType();

            inWeb |= material.equals(Material.WEB);
        }

        // ---{多个方向的碰撞处理}---
        nearStep = pushedByPiston = false;
        // -----------------------

        boolean onFence = !fence.checkMaterials(material -> !material.toString().contains("FENCE")
                && !material.equals(Material.COBBLE_WALL)); // 在高0.5格碰撞的方块上

        // --------------------------------------------------
        // 脚下
        onSlime = false;

        for (Block block : under.getBlocks()) {
            Material material = block.getType();

            onSlime |= material.equals(Material.SLIME_BLOCK);

            nearStep |= material.toString().contains("STAIRS"); // 修复一些只有楼梯会误判的问题

            // 多个方向的碰撞处理
            pushedByPiston |= material.equals(Material.PISTON_MOVING_PIECE) || material.equals(Material.PISTON_EXTENSION);
        }

        // --------------------------------------------------
        // 头上
        underBlock = false;

        for (Block block : above.getBlocks()) {
            Material material = block.getType();

            underBlock |= !BlockUtil.PASSABLE.contains(material) && !block.isLiquid() && !material.equals(Material.WEB);

            // 多个方向的碰撞处理
            pushedByPiston |= material.equals(Material.PISTON_MOVING_PIECE) || material.equals(Material.PISTON_EXTENSION);
        }

        // --------------------------------------------------
        // 边上
        nearWall = false;

        for (Block block : near.getBlocks()) {
            Material material = block.getType();

            nearWall |= !block.isEmpty() && !block.isLiquid() && !material.equals(Material.WEB);

            nearStep |= BlockUtil.STEP_ABLE.contains(material);

            // 多个方向的碰撞处理
            pushedByPiston |= material.equals(Material.PISTON_MOVING_PIECE);
        }

        // --------------------------------------------------
        // 由于游戏机制问题需要单独计算

        // 客户端会先更新Y轴, 所以需要使用上一次的XZ位置与当前的Y位置
        Location lastLoc = lastLocation.clone();
        lastLoc.setY(location.getY());

        // 会因未更新位置导致存在约0.015555或0.0216626左右的Y轴偏差, 也可能因为下落速度快而增大误差, 给0.03较为稳妥
        Cuboid ground = new Cuboid(lastLoc).expandXZ(0.3F).expandY(updatePos ? 0.000001 : 0.03, 0.0F);

        serverGround = !ground.checkBlocks(block -> block.isEmpty() || block.isLiquid()
                || block.getType().equals(Material.WEB)) || onFence;
    }

    private void handleCollisions() {
        handleCuboid();

        handle_climbable:
        {
            Block block = BlockUtil.getBlockAsync(new Location(location.getWorld(),
                    Math.floor(location.getX()), Math.floor(location.getY()), Math.floor(location.getZ())));

            if (block == null) break handle_climbable;

            Material material = block.getType();

            climbing = material == Material.LADDER || material == Material.VINE;
        }

        nearVehicle = nearBoat = false;

        handle_nearByEntities:
        {
            if (isInUnloadedChunk()) break handle_nearByEntities;

            List<Entity> nearByEntities = null;

            try {
                nearByEntities = player.getNearbyEntities(2.5, 2.5, 2.5);
            } catch (Exception ignore) {
            }

            if (nearByEntities == null || nearByEntities.isEmpty()) break handle_nearByEntities;

            // 不加上面那些判断可能会报错, 因为FF不在主线程上运行

            for (Entity entity : nearByEntities) {
                nearVehicle |= entity instanceof Vehicle;
                nearBoat |= entity.getType() == EntityType.BOAT;
            }
        }

        // 上下船处理详见EventListener中50行与59行

        if (inVehicle && !nearVehicle) transConfirm(() -> inVehicle = false); // 例如: 船被指令杀死

        inVehicle |= player.isInsideVehicle(); // 修复 Augustus 的 Intave Boat Disabler

        nearStep |= nearBoat; // 船高度为0.6, 玩家刚好可以登上, 所以也算在台阶内

        boolean onGhostBlock = false;

        Location lastLoc = lastLocation.clone();
        lastLoc.setY(location.getY());

        BoundingBox playerBox = new BoundingBox(lastLoc.toVector());

        for (BlockPosition block : ghostBlocks.keySet()) {
            Cuboid blockBox = new Cuboid(player.getWorld(), block);

            // 检查玩家是否在其放置的方块上
            onGhostBlock |= Math.abs(playerBox.getMinY() - blockBox.getMaxY()) <= (updatePos ? 1.0 : 1.03)
                    && playerBox.getMinX() < blockBox.getMaxX()
                    && playerBox.getMaxX() > blockBox.getMinX()
                    && playerBox.getMinZ() < blockBox.getMaxZ()
                    && playerBox.getMaxZ() > blockBox.getMinZ();
        }

        serverGround |= onGhostBlock;

        lastFriction = friction;
        friction = computeFriction();
    }

    @SuppressWarnings("deprecation")
    public void transConfirm(Runnable runnable) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.TRANSACTION);

        packet.getIntegers().write(0, 0);
        packet.getShorts().write(0, transId);
        packet.getBooleans().write(0, false);

        sendPacket(packet);

        actionMap.put(transId, runnable);

        short newId; // 使用随机ID, 兼容其它反作弊, 也可以防止作弊端预测

        do {
            newId = (short) -ThreadLocalRandom.current().nextInt(32768);
        } while (newId == transId);

        // 一般反作弊使用的id范围是 -32767 到 0, 因为原版服务器是从 1 开始发的

        transId = newId;

        lastSentTransaction = System.currentTimeMillis();
    }

    public void randomChangeSlot() { // 切换到其它栏位再切回, 用以刷新物品的使用状态, 缓解玩家的行为与服务器失去同步时造成的误判
        if (inSlotChanging) return; // 防止同时发送多次切换导致客户端来不及响应而误判
        inSlotChanging = true;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.HELD_ITEM_SLOT);

        int originalSlot = itemSlot, randomSlot;

        do {
            randomSlot = new Random().nextInt(9); // 栏位范围: 0-8
        } while (randomSlot == originalSlot); // 一样的栏位没有刷新物品状态的效果

        packet.getIntegers().write(0, randomSlot);
        sendPacket(packet);

        Bukkit.getScheduler().runTaskLater(FairFight.INSTANCE.getPlugin(), () -> {
            packet.getIntegers().write(0, originalSlot);
            sendPacket(packet); // 将栏位改写为最初的栏位再次发送回去

            inSlotChanging = false;
        }, 3L);
    }

    public void closeInventory() {
        player.closeInventory();

        inventoryOpen = false;
    }

    public void resetSneak() {
        sneaking = false;

        player.setSneaking(false);
    }

    private void sendPacket(PacketContainer container) {
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
    }

    public void confirmConnection() {
        long now = System.currentTimeMillis();

        if (now - lastSentTransaction > 1000L) transConfirm(() -> {/*仅测延迟*/});

        if (!ConfigValue.timeout_check_enabled || bypass || kicked
                || FairFight.INSTANCE.getServerTickTask().isLagging()) return;

        check_transaction:
        {
            if (!ConfigValue.timeout_check_transaction_enabled) break check_transaction;

            long delay = now - lastRepliedTransaction;

            if (delay > ConfigValue.timeout_check_transaction_max_delay) {
                timeoutCheckAlert(
                        ConfigValue.timeout_check_transaction_kick_message,
                        ConfigValue.timeout_check_transaction_alert_message,
                        delay
                );

                return; // 踢一个就行, 不用走下面的了
            }
        }

        check_flying:
        {
            if (!ConfigValue.timeout_check_flying_enabled) break check_flying;

            long delay = now - lastFlyingTime;

            if (delay > ConfigValue.timeout_check_flying_max_delay) {
                timeoutCheckAlert(
                        ConfigValue.timeout_check_flying_kick_message,
                        ConfigValue.timeout_check_flying_alert_message,
                        delay
                );
            }
        }

    }

    private void timeoutCheckAlert(String kickMessage, String alertMessage, long delay) {
        kicked = true;

        FairFight.INSTANCE.sendToMainThread(() -> player.kickPlayer(StringUtil
                .color(kickMessage.replace("%prefix%", FairFight.PREFIX))));

        String message = StringUtil.color(alertMessage
                .replace("%prefix%", FairFight.PREFIX)
                .replace("%player%", player.getName())
                .replace("%delay%", String.valueOf(delay))
        );

        FairFight.INSTANCE.getDataManager().toStaff(staff -> staff.getPlayer().sendMessage(message));

        if (ConfigValue.alerts_print_to_console) Bukkit.getConsoleSender().sendMessage(message);
    }

    public boolean testJumped() {
        boolean leftGround = lastClientGround && !clientGround;

        boolean math = deltaY % 0.015625 == 0.0 && lastDeltaY % 0.015625 == 0.0;

        double threshold = isOffsetYMotion() ? 0.05 : 1E-5;

        return Math.abs(getAttributeJump() - deltaY) < threshold && leftGround && !math;
    }

    public float getAttributeSpeed() {
        float attribute = walkSpeed;

        if (sprinting) attribute *= 1.3F;

        attribute *= 1.0F + (speedEffect * 0.2F);
        attribute *= 1.0F + (slowEffect * -0.15F);

        if (wasInWater) { // 修复深海探索者附魔误判
            float depthStrider = 0.0F;

            ItemStack boots = player.getInventory().getBoots();

            if (boots != null) depthStrider = boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);

            if (depthStrider > 3.0F) depthStrider = 3.0F;

            if (depthStrider > 0.0F) attribute *= 1.0F + 0.3333333F * depthStrider;
        }

        return attribute;
    }

    public float getAttributeJump() {
        return 0.42F + (jumpEffect * 0.1F);
    }

    private float computeFriction() { // 摩擦力计算来自Hawk. 检测需调用上一次的摩擦(lastFriction)
        if (inWater && !isFlying) {
            float friction = 0.8F, depthStrider = 0.0F;

            ItemStack boots = player.getInventory().getBoots();

            if (boots != null) depthStrider = boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);

            if (depthStrider > 3.0F) depthStrider = 3.0F;

            if (!clientGround) depthStrider *= 0.5F;

            if (depthStrider > 0.0F) friction += (0.546F - friction) * depthStrider / 3.0F;

            return friction;
        } else if (inLava && !isFlying) return 0.5F;
        else {
            float friction = 0.91F;

            if (clientGround) {
                Block blockBelow = BlockUtil.getBlockAsync(new Location(player.getWorld(),
                        location.getX(), location.getY() - 1.0, location.getZ()
                ));

                if (blockBelow != null) friction *= BlockUtil.getSlipperiness(blockBelow.getType());
            }

            return friction;
        }
    }

    public boolean isOnEdge(double y) {
        Block block = BlockUtil.getBlockAsync(location.clone().subtract(0.0, y, 0.0));

        return block != null && block.getType() == Material.AIR;
    }

    public boolean isBridging() {
        boolean lookDown = Math.max(location.getPitch(), lastLocation.getPitch()) > 50.0F;
        boolean placing = tickSincePlacedBlock < 5;

        return isOnEdge(2.0) && lookDown && placing;
    }

    private boolean canBlock(Material material) {
        return material.name().contains("SWORD");
    }

    private boolean canEat(ItemStack itemStack) {
        Material material = itemStack.getType();

        boolean hungry = player.getFoodLevel() < 20;
        boolean gApple = material.equals(Material.GOLDEN_APPLE);
        boolean milk = material.equals(Material.MILK_BUCKET);
        boolean potion = material.equals(Material.POTION);

        return (material.isEdible() && (hungry || gApple) && !instantlyBuild)
                || milk || (potion && itemStack.getDurability() == 0)
                || (potion && !Potion.fromItemStack(itemStack).isSplash());
    }

    private boolean canDrawBow(Material material) {
        return material.equals(Material.BOW) && player.getInventory().contains(Material.ARROW);
    }

    public void destroy() {
        checks.clear();
        entityMap.clear();
        ghostBlocks.clear();
        teleports.clear();
        transactionMap.clear();
        actionMap.clear();

        lastLastLocation = lastLocation = null;
        lastTarget = null;
    }

    public void resetVL() {
        checks.forEach(Check::resetViolations);
    }

    public boolean isInUnloadedChunk() {
        return !ServerUtil.isChunkLoaded(location);
    }

    public Player getPlayer() {
        return player;
    }

    public EmulationProcessor getEmulationProcessor() {
        return emulationProcessor;
    }

    public Set<BlockPosition> getGhostBlocks() {
        return ghostBlocks.keySet();
    }

    public boolean isAlerts() {
        return alerts;
    }

    public void setAlerts(boolean alerts) {
        this.alerts = alerts;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setBypass(boolean bypass) {
        if (ConfigValue.ignore_high_version && FairFight.isViaVersionEnabled) {
            if (ViaVersionUtil.isPlayerHighVersion(this.player)) {
                this.bypass = true;
                return;
            }
        }
        this.bypass = bypass;
    }

    public boolean isPunished() {
        return punished;
    }

    public void setPunished(boolean punished) {
        this.punished = punished;
    }

    public List<Check> getChecks() {
        return checks;
    }

    public Map<Integer, HitboxEntity> getEntityMap() {
        return entityMap;
    }

    public Player getLastTarget() {
        return lastTarget;
    }

    public boolean maybeLagging() { // 注意! 这个判断不可信, 轻松被绕过(FakeLag, Blink等)
        long delta = flyingTime - lastFlyingTime;

        return delta > 100 || delta < 2;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public boolean isInstantlyBuild() {
        return instantlyBuild;
    }

    public boolean isInVehicle() {
        return inVehicle;
    }

    public void setInVehicle(boolean inVehicle) {
        this.inVehicle = inVehicle;
    }

    public boolean isLastLastClientGround() {
        return lastLastClientGround;
    }

    public boolean isLastClientGround() {
        return lastClientGround;
    }

    public boolean isClientGround() {
        return clientGround;
    }

    public boolean isMathGround() {
        return mathGround;
    }

    public Location getLastLastLocation() {
        return lastLastLocation.clone();
    }

    public Location getLastLocation() {
        return lastLocation.clone();
    }

    public Location getLocation() {
        return location.clone();
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isJumped() {
        return jumped;
    }

    public void resetUsingItem() {
        blocking = eating = drawingBow = false;
    }

    public boolean isUsingItem() {
        return blocking || eating || drawingBow;
    }

    public boolean isEating() {
        return eating;
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public double getLastLastDeltaY() {
        return lastLastDeltaY;
    }

    public double getLastDeltaX() {
        return lastDeltaX;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public double getLastDeltaZ() {
        return lastDeltaZ;
    }

    public double getLastDeltaXZ() {
        return lastDeltaXZ;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public double getDeltaZ() {
        return deltaZ;
    }

    public double getDeltaXZ() {
        return deltaXZ;
    }

    public float getLastLastDeltaYaw() {
        return lastLastDeltaYaw;
    }

    public float getLastLastDeltaPitch() {
        return lastLastDeltaPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public float getLastDeltaPitch() {
        return lastDeltaPitch;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public float getAccelYaw() {
        return accelYaw;
    }

    public float getAccelPitch() {
        return accelPitch;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public double getVelocityXZ() {
        return velocityXZ;
    }

    public int getSpeedEffect() {
        return speedEffect;
    }

    public int getSlowEffect() {
        return slowEffect;
    }

    public int getJumpEffect() {
        return jumpEffect;
    }

    public int getItemSlot() {
        return itemSlot;
    }

    public float getFriction() {
        return lastFriction;
    }

    public boolean isOffsetMotion() {
        return tickSinceOffsetMotion <= 21;
    }

    public boolean isOffsetYMotion() {
        return isOffsetMotion() || deltaXZ < 0.05;
    }

    public boolean isServerGround() {
        return serverGround;
    }

    public boolean isInWater() {
        return wasInWater;
    }

    public boolean isInLava() {
        return wasInLava;
    }

    public boolean isInFlowingWater() {
        return wasInFlowingWater;
    }

    public boolean isInFlowingLava() {
        return wasInFlowingLava;
    }

    public boolean isInLiquid() {
        return wasInWater || wasInLava;
    }

    public boolean isInFlowingLiquid() {
        return wasInFlowingWater || wasInFlowingLava;
    }

    public boolean isUnderBlock() {
        return underBlock;
    }

    public boolean isInWeb() {
        return wasInWeb;
    }

    public boolean isNearWall() {
        return nearWall;
    }

    public boolean isPushedByPiston() {
        return pushedByPiston;
    }

    public boolean isClimbing() {
        return climbing;
    }

    public boolean isNearVehicle() {
        return nearVehicle;
    }

    public boolean isNearBoat() {
        return nearBoat;
    }

    public boolean isNearStep() {
        return nearStep;
    }

    public boolean isOnSlime() {
        return wasOnSlime;
    }

    public boolean isPlacing() {
        return placing;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isDigging() {
        return digging;
    }

    public long getTransPing() {
        return transPing;
    }

    public int getPingTicks() {
        return (int) Math.round(transPing / 50.0);
    }

    public int getTick() {
        return tick;
    }

    public int getTickSinceAbilityChange() {
        return tickSinceAbilityChange;
    }

    public int getTickSinceSteerVehicle() {
        return tickSinceSteerVehicle;
    }

    public int getInventoryOpenTicks() {
        return inventoryOpenTicks;
    }

    public int getTickSinceRidingInteract() {
        return tickSinceRidingInteract;
    }

    public int getTickSincePlacedBlock() {
        return tickSincePlacedBlock;
    }

    public int getTickSinceDroppedItem() {
        return tickSinceDroppedItem;
    }

    public int getTickSinceTeleport() {
        return tickSinceTeleport;
    }

    public int getTickSinceClientGround() {
        return tickSinceClientGround;
    }

    public int getTickSinceSprinting() {
        return tickSinceSprinting;
    }

    public int getTickSinceJumped() {
        return tickSinceJumped;
    }

    public int getTickSinceUsingItem() {
        return tickSinceUsingItem;
    }

    public int getTickSinceAttack() {
        return tickSinceAttack;
    }

    public int getTickSinceVelocity() {
        return tickSinceVelocity;
    }

    public int getTickSinceOtherVelocity() {
        return tickSinceOtherVelocity;
    }

    public int getMaxVelocityTicks() {
        return maxVelocityTicks;
    }

    public int getClimbingTicks() {
        return climbingTicks;
    }

    public int getTickSinceClimbing() {
        return tickSinceClimbing;
    }

    public int getTickSinceInLiquid() {
        return tickSinceInLiquid;
    }

    public int getTickSinceInFlowingLava() {
        return tickSinceInFlowingLava;
    }

    public int getLiquidTicks() {
        return liquidTicks;
    }

    public int getTickSinceInWeb() {
        return tickSinceInWeb;
    }

    public int getTickSinceNearStep() {
        return tickSinceNearStep;
    }

    public int getTickSincePushedByPiston() {
        return tickSincePushedByPiston;
    }

    public int getTickSinceUnderBlock() {
        return tickSinceUnderBlock;
    }

    public int getTickSinceOnSlime() {
        return tickSinceOnSlime;
    }

    public int getTickSinceNearWall() {
        return tickSinceNearWall;
    }

    public int getTickSinceServerGround() {
        return tickSinceServerGround;
    }
}
