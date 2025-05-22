package net.mca.server;

import net.mca.Config;
import net.mca.ducks.IVillagerEntity;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.VillagerFactory;
import net.mca.entity.ZombieVillagerEntityMCA;
import net.mca.entity.ZombieVillagerFactory;
import net.mca.entity.ai.relationship.Gender;
import net.mca.server.world.data.Nationality;
import net.mca.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpawnQueue {
    private static final SpawnQueue INSTANCE = new SpawnQueue();

    public static SpawnQueue getInstance() {
        return INSTANCE;
    }

    private final ConcurrentLinkedQueue<Villager> villagerSpawnQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ZombieVillager> zombieVillagerSpawnQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Zombie> zombieSpawnList = new ConcurrentLinkedQueue<>();

    public static final TicketType<BlockPos> SPAWN = TicketType.create("mca:spawner", Vec3i::compareTo, 1);

    private void lock(Entity entity) {
        if (entity.level() instanceof ServerLevel world) {
            ServerChunkCache chunkManager = world.getChunkSource();
            ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
            chunkManager.addRegionTicket(SPAWN, chunkPos, 8, entity.blockPosition());
        }
    }

    private void unlock(Entity entity) {
        if (entity.level() instanceof ServerLevel world) {
            ServerChunkCache chunkManager = world.getChunkSource();
            ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
            chunkManager.removeRegionTicket(SPAWN, chunkPos, 8, entity.blockPosition());
        }
    }

    public void tick() {
        // lazy spawning of our villagers as they can't be spawned while loading
        Villager ve = villagerSpawnQueue.poll();
        if (ve != null) {
            lock(ve);
            if (WorldUtils.isChunkLoaded(ve.level(), ve.blockPosition())) {
                ve.discard();
                VillagerEntityMCA villager = VillagerFactory.newVillager(ve.level())
                        .withName(ve.hasCustomName() ? ve.getName().getString() : null)
                        .withGender(Gender.getRandom())
                        .withAge(ve.getAge())
                        .withPosition(ve)
                        .withType(ve.getVillagerData().getType())
                        .withProfession(ve.getVillagerData().getProfession(), ve.getVillagerData().getLevel(), ve.getOffers())
                        .spawn(((IVillagerEntity) ve).getSpawnReason());

                copyPastaIntensifies(villager, ve);
            } else {
                villagerSpawnQueue.add(ve);
            }
            unlock(ve);
        }

        ZombieVillager zve = zombieVillagerSpawnQueue.poll();
        if (zve != null) {
            lock(zve);
            if (WorldUtils.isChunkLoaded(zve.level(), zve.blockPosition())) {
                zve.discard();
                ZombieVillagerEntityMCA villager = ZombieVillagerFactory.newVillager(zve.level())
                        .withName(zve.hasCustomName() ? zve.getName().getString() : null)
                        .withGender(Gender.getRandom())
                        .withPosition(zve)
                        .withType(zve.getVillagerData().getType())
                        .withProfession(zve.getVillagerData().getProfession(), zve.getVillagerData().getLevel())
                        .spawn(((IVillagerEntity) zve).getSpawnReason());

                copyPastaIntensifies(villager, zve);
            } else {
                zombieVillagerSpawnQueue.add(zve);
            }
            unlock(zve);
        }

        Zombie ze = zombieSpawnList.poll();
        if (ze != null) {
            lock(ze);
            if (WorldUtils.isChunkLoaded(ze.level(), ze.blockPosition())) {
                ze.discard();
                ZombieVillagerEntityMCA villager = ZombieVillagerFactory.newVillager(ze.level())
                        .withName(ze.hasCustomName() ? ze.getName().getString() : null)
                        .withGender(Gender.getRandom())
                        .withPosition(ze)
                        .withType(VillagerType.byBiome(ze.level().getBiome(ze.blockPosition())))
                        .withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getRandom(ze.getRandom()).map(Holder::value).orElse(VillagerProfession.NONE))
                        .spawn(MobSpawnType.NATURAL);

                copyPastaIntensifies(villager, ze);
            } else {
                zombieSpawnList.add(ze);
            }
            unlock(ze);
        }
    }

    private void copyPastaIntensifies(PathfinderMob villager, PathfinderMob entity) {
        if (entity.isPersistenceRequired()) {
            villager.setPersistenceRequired();
        }
        if (entity.isInvulnerable()) {
            villager.setInvulnerable(true);
        }
        if (entity.isNoAi()) {
            villager.setNoAi(true);
        }

        for (String tag : entity.getTags()) {
            villager.addTag(tag);
        }
    }

    public static boolean shouldGetConverted(Entity entity) {
        if (Config.getInstance().fractionOfVanillaVillages <= 0) {
            return true;
        } else {
            int i = Nationality.get((ServerLevel) entity.level()).getRegionId(entity.blockPosition());
            return Math.floorMod(i, 100) >= Config.getInstance().fractionOfVanillaVillages * 100.0;
        }
    }

    public boolean addVillager(Entity entity) {
        if (entity instanceof IVillagerEntity villagerEntity && !handlesSpawnReason(villagerEntity.getSpawnReason())) {
            return false;
        }
        if (Config.getInstance().villagerDimensionBlacklist.contains(entity.getCommandSenderWorld().dimension().location().toString())) {
            return false;
        }
        if (Config.getInstance().overwriteOriginalVillagers
                && (entity.getClass().equals(Villager.class) ||
                Config.getInstance().moddedVillagerWhitelist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()) && entity instanceof Villager)
                && shouldGetConverted(entity)
                && !villagerSpawnQueue.contains(entity)) {
            return villagerSpawnQueue.add((Villager) entity);
        }
        if (Config.getInstance().overwriteOriginalZombieVillagers
                && (entity.getClass().equals(ZombieVillager.class) ||
                Config.getInstance().moddedZombieVillagerWhitelist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()) && entity instanceof ZombieVillager)
                && Config.getInstance().fractionOfVanillaZombies < ((ZombieVillager) entity).getRandom().nextFloat()
                && !zombieVillagerSpawnQueue.contains(entity)) {
            return zombieVillagerSpawnQueue.add((ZombieVillager) entity);
        }
        if (Config.getInstance().overwriteAllZombiesWithZombieVillagers
                && entity.getClass().equals(Zombie.class)
                && !zombieSpawnList.contains(entity)) {
            return zombieSpawnList.add((Zombie) entity);
        }
        return false;
    }

    private boolean handlesSpawnReason(MobSpawnType reason) {
        return Config.getInstance().allowedSpawnReasons.contains(reason.name().toLowerCase(Locale.ROOT));
    }

    public void convert(Villager villager) {
        villagerSpawnQueue.add(villager);
    }
}
