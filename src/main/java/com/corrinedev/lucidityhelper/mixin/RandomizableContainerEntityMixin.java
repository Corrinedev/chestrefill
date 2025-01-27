package com.corrinedev.lucidityhelper.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerEntityMixin extends BaseContainerBlockEntity {
    @Shadow @Nullable protected ResourceLocation lootTable;
    @Shadow protected long lootTableSeed;

    @Shadow public abstract void clearContent();

    @Unique public ResourceLocation savedLootTable;
    @Unique public long lastTimestamp = 0;

    protected RandomizableContainerEntityMixin(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
        super(p_155076_, p_155077_, p_155078_);
    }

    @Inject(method = "tryLoadLootTable", at = @At("RETURN"))
    public void saveLootTable(CompoundTag p_59632_, CallbackInfoReturnable<Boolean> cir) {
        this.savedLootTable = lootTable;
        this.lastTimestamp = p_59632_.getLong("LastTimeStamp");
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    protected boolean trySaveLootTable(CompoundTag p_59635_) {
        if (this.savedLootTable == null) {
            return false;
        } else {
            p_59635_.putString("LootTable", this.savedLootTable.toString());
            p_59635_.putLong("LastTimeStamp", this.lastTimestamp);
            if (this.lootTableSeed != 0L) {
                p_59635_.putLong("LootTableSeed", this.lootTableSeed);
            }

            return true;
        }
    }
    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    public void saveLootTable(ResourceLocation p_59627_, long p_59628_, CallbackInfo ci) {
        this.savedLootTable = lootTable;
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void unpackLootTable(@Nullable Player p_59641_) {
        if(Math.abs(this.lastTimestamp - System.currentTimeMillis()) >= 10000) this.lootTable = savedLootTable;

        if (this.lootTable != null && this.level.getServer() != null) {
            clearContent();

            this.lastTimestamp = System.currentTimeMillis();
            this.getPersistentData().putLong("LastTimeStamp", this.lastTimestamp);
            LootTable $$1 = this.level.getServer().getLootData().getLootTable(this.lootTable);
            if (p_59641_ instanceof ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)p_59641_, this.lootTable);
            }

            this.lootTable = null;

            LootParams.Builder $$2 = (new LootParams.Builder((ServerLevel)this.level)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition));
            if (p_59641_ != null) {
                $$2.withLuck(p_59641_.getLuck()).withParameter(LootContextParams.THIS_ENTITY, p_59641_);
            }

            $$1.fill(this, $$2.create(LootContextParamSets.CHEST), this.lootTableSeed);
        }

    }
}
