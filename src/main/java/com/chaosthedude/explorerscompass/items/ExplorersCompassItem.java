package com.chaosthedude.explorerscompass.items;

import java.util.*;
import java.util.stream.Collectors;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.cache.PlayerStructureCache;
import com.chaosthedude.explorerscompass.cache.SharedStructureCache;
import com.chaosthedude.explorerscompass.cache.StructureLocation;
import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.gui.GuiWrapper;
import com.chaosthedude.explorerscompass.network.SyncPacket;
import com.chaosthedude.explorerscompass.util.CompassState;
import com.chaosthedude.explorerscompass.util.ItemUtils;
import com.chaosthedude.explorerscompass.util.PlayerUtils;
import com.chaosthedude.explorerscompass.util.StructureUtils;
import com.chaosthedude.explorerscompass.worker.SearchWorkerManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.NotNull;

public class ExplorersCompassItem extends Item {

	public static final String NAME = "explorerscompass";
	
	private final SearchWorkerManager workerManager;

	public ExplorersCompassItem() {
		super(new Properties().stacksTo(1));
		workerManager = new SearchWorkerManager();
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		if (!player.isCrouching()) {
			if (level.isClientSide()) {
				final ItemStack stack = ItemUtils.getHeldItem(player, ExplorersCompass.explorersCompass);
				GuiWrapper.openGUI(level, player, stack);
			} else {
				final ServerLevel serverLevel = (ServerLevel) level;
				final ServerPlayer serverPlayer = (ServerPlayer) player;
				final boolean canTeleport = ConfigHandler.GENERAL.allowTeleport.get() && PlayerUtils.canTeleport(player.getServer(), player);
				ExplorersCompass.network.sendTo(new SyncPacket(canTeleport, StructureUtils.getAllowedStructureKeys(serverLevel), StructureUtils.getGeneratingDimensionsForAllowedStructures(serverLevel), StructureUtils.getStructureKeysToTypeKeys(serverLevel), StructureUtils.getTypeKeysToStructureKeys(serverLevel)), serverPlayer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
			}
		} else {
			workerManager.stop();
			workerManager.clear();
			setState(player.getItemInHand(hand), null, CompassState.INACTIVE, player);
		}
		return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, player.getItemInHand(hand));
	}
	
	@Override
 	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
 		if (getState(oldStack) == getState(newStack)) {
 			return false;
 		}
 		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
 	}

	public void searchForStructure(Level level, Player player, ResourceLocation categoryKey,
								   List<ResourceLocation> structureKeys, BlockPos pos, ItemStack stack,
								   boolean ignoreOldExplored, boolean ignoreOthersExplored) {

		setSearching(stack, categoryKey, player);
		setSearchRadius(stack, 0, player);

		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			UUID playerUuid = serverPlayer.getUUID();

			// 获取玩家个人缓存
			Set<StructureLocation> myCache = PlayerStructureCache.getCache(serverLevel, playerUuid);

			List<Structure> structures = new ArrayList<>();
			for (ResourceLocation key : structureKeys) {
				structures.add(StructureUtils.getStructureForKey(serverLevel, key));
			}

			workerManager.stop();
			workerManager.createWorkers(serverLevel, player, stack, structures, pos,
					(foundPos, foundStructure) -> {
						// 创建结构位置对象
						StructureLocation foundLoc = new StructureLocation(
								StructureUtils.getKeyForStructure(serverLevel, foundStructure),
								serverLevel.dimension().location().toString(),
								foundPos.getX(), foundPos.getZ()
						);

						// 检查是否忽略已探索结构
						if (ignoreOldExplored && myCache.contains(foundLoc)) {
							return false; // 跳过自己已探索的结构
						}

						// 从公共缓存检查其他玩家是否探索过
						Set<UUID> explorers = SharedStructureCache.getExplorers(foundLoc);
						if (!explorers.isEmpty()) {
							boolean hasOtherExplorers = explorers.stream().anyMatch(uuid -> !uuid.equals(playerUuid));
							if (hasOtherExplorers) {
								if (ignoreOthersExplored) {
									return false; // 跳过其他玩家已探索的结构
								} else {
									// 获取最后3个探索者的名称
									List<String> lastExplorerNames = explorers.stream()
											.limit(3)
											.map(PlayerStructureCache::getName)
											.filter(name -> !name.equals("Unknown"))
											.collect(Collectors.toList());

									if (!lastExplorerNames.isEmpty()) {
										// 格式化探索者名单
										String explorersList = String.join(", ", lastExplorerNames);
										if (explorers.size() > 3) {
											explorersList += " 等" + explorers.size() + "人";
										}

										// 提示该结构已被其他玩家探索
										showExploredByOtherPlayers(serverPlayer, explorersList, foundLoc);
										setOtherPlayerNames(stack, explorersList);
									}
								}

							}
						}

						// 添加到个人缓存和公共缓存
						PlayerStructureCache.addToCache(serverPlayer, foundLoc);
						SharedStructureCache.addToSharedCache(serverPlayer, foundLoc);

						return true;
					}
			);

			boolean started = workerManager.start();
			if (!started) {
				setNotFound(stack, 0, 0);
			}
		}
	}

	private void showExploredByOtherPlayers(ServerPlayer player, String otherPlayerName, StructureLocation loc) {
		String i18nKey = "structure." + loc.structureKey.getNamespace() + "." + loc.structureKey.getPath();
		Component localizedName = Component.translatable(i18nKey);
		player.sendSystemMessage(Component.literal("本次搜索命中的结构")
				.append(localizedName)
				.append(String.format(" X=%d - Z=%d，已被玩家%s搜索", loc.x, loc.z, otherPlayerName))
		);
	}
	public void setOtherPlayerNames(ItemStack stack, String otherPlayerName) {
		if (ItemUtils.verifyNBT(stack)) {
            if (stack.getTag() != null) {
                stack.getTag().putString("otherPlayer", otherPlayerName);
            }
        }
	}
	public void succeed(ItemStack stack, ResourceLocation structureKey, int x, int z, int samples, boolean displayCoordinates) {
		setFound(stack, structureKey, x, z, samples);
		setDisplayCoordinates(stack, displayCoordinates);
		workerManager.clear();
	}
	
	public void fail(ItemStack stack, int radius, int samples) {
		workerManager.pop();
		boolean started = workerManager.start();
		if (!started) {
			setNotFound(stack, radius, samples);
		}
	}

	public boolean isActive(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return getState(stack) != CompassState.INACTIVE;
		}

		return false;
	}

	public void setSearching(ItemStack stack, ResourceLocation structureKey, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putString("StructureKey", structureKey.toString());
			stack.getTag().putInt("State", CompassState.SEARCHING.getID());
		}
	}

	public void setFound(ItemStack stack, ResourceLocation structureKey, int x, int z, int samples) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("State", CompassState.FOUND.getID());
			stack.getTag().putString("StructureKey", structureKey.toString());
			stack.getTag().putInt("FoundX", x);
			stack.getTag().putInt("FoundZ", z);
			stack.getTag().putInt("Samples", samples);
		}
	}

	public void setNotFound(ItemStack stack, int searchRadius, int samples) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("State", CompassState.NOT_FOUND.getID());
			stack.getTag().putInt("SearchRadius", searchRadius);
			stack.getTag().putInt("Samples", samples);
		}
	}

	public void setInactive(ItemStack stack, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("State", CompassState.INACTIVE.getID());
		}
	}

	public void setState(ItemStack stack, BlockPos pos, CompassState state, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("State", state.getID());
		}
	}

	public void setFoundStructureX(ItemStack stack, int x, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("FoundX", x);
		}
	}

	public void setFoundStructureZ(ItemStack stack, int z, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("FoundZ", z);
		}
	}

	public void setStructureKey(ItemStack stack, ResourceLocation structureKey, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putString("StructureKey", structureKey.toString());
		}
	}

	public void setSearchRadius(ItemStack stack, int searchRadius, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("SearchRadius", searchRadius);
		}
	}

	public void setSamples(ItemStack stack, int samples, Player player) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putInt("Samples", samples);
		}
	}
	
	public void setDisplayCoordinates(ItemStack stack, boolean displayPosition) {
		if (ItemUtils.verifyNBT(stack)) {
			stack.getTag().putBoolean("DisplayCoordinates", displayPosition);
		}
	}

	public CompassState getState(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return CompassState.fromID(stack.getTag().getInt("State"));
		}

		return null;
	}

	public int getFoundStructureX(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return stack.getTag().getInt("FoundX");
		}

		return 0;
	}

	public int getFoundStructureZ(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return stack.getTag().getInt("FoundZ");
		}

		return 0;
	}

	public ResourceLocation getStructureKey(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return new ResourceLocation(stack.getTag().getString("StructureKey"));
		}

		return new ResourceLocation("");
	}

	public int getSearchRadius(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return stack.getTag().getInt("SearchRadius");
		}

		return -1;
	}

	public int getSamples(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack)) {
			return stack.getTag().getInt("Samples");
		}

		return -1;
	}

	public int getDistanceToBiome(Player player, ItemStack stack) {
		return StructureUtils.getHorizontalDistanceToLocation(player, getFoundStructureX(stack), getFoundStructureZ(stack));
	}
	
	public boolean shouldDisplayCoordinates(ItemStack stack) {
		if (ItemUtils.verifyNBT(stack) && stack.getTag().contains("DisplayCoordinates")) {
			return stack.getTag().getBoolean("DisplayCoordinates");
		}

		return true;
	}

}
