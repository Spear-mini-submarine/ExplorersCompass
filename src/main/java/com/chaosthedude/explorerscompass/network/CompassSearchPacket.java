package com.chaosthedude.explorerscompass.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.items.ExplorersCompassItem;
import com.chaosthedude.explorerscompass.util.ItemUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class CompassSearchPacket {

	private ResourceLocation groupKey;
	private List<ResourceLocation> structureKeys;
	private int x;
	private int y;
	private int z;
	private boolean ignoreOldExplored;
	private boolean ignoreOthersExplored;
	public CompassSearchPacket() {}

	public CompassSearchPacket(ResourceLocation groupKey, List<ResourceLocation> structureKeys, BlockPos pos,boolean ignoreOldExplored,boolean ignoreOthersExplored) {
		this.groupKey = groupKey;
		this.structureKeys = structureKeys;

		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();

		this.ignoreOldExplored = ignoreOldExplored;
		this.ignoreOthersExplored = ignoreOthersExplored;
	}

	public CompassSearchPacket(FriendlyByteBuf buf) {
		groupKey = buf.readResourceLocation();
		
		structureKeys = new ArrayList<ResourceLocation>();
		int numStructures = buf.readInt();
		for (int i = 0; i < numStructures; i++) {
			structureKeys.add(buf.readResourceLocation());
		}

		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();

		ignoreOldExplored = buf.readBoolean();
		ignoreOthersExplored = buf.readBoolean();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeResourceLocation(groupKey);
		
		buf.writeInt(structureKeys.size());
		for (ResourceLocation key : structureKeys) {
			buf.writeResourceLocation(key);
		}

		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);

		buf.writeBoolean(ignoreOldExplored);
		buf.writeBoolean(ignoreOthersExplored);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(ctx.get().getSender(), ExplorersCompass.explorersCompass);
			if (!stack.isEmpty()) {
				final ExplorersCompassItem explorersCompass = (ExplorersCompassItem) stack.getItem();
				explorersCompass.searchForStructure(
						ctx.get().getSender().serverLevel(),
						ctx.get().getSender(),
						groupKey,
						structureKeys,
						new BlockPos(x, y, z),
						stack,
						ignoreOldExplored,
						ignoreOthersExplored);
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
