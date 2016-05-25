package src.train.common.tile;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import src.train.common.blocks.BlockOpenHearthFurnace;
import src.train.common.library.BlockIDs;
import src.train.common.library.ItemIDs;
import src.train.common.recipes.OpenHearthFurnaceRecipes;

import java.util.Random;

public class TileEntityOpenHearthFurnace extends TileTraincraft{

	private ForgeDirection facing;
	public int furnaceBurnTime;
	public int currentItemBurnTime;
	public int furnaceCookTime;
	private int cookDuration;
	private Random random;

	public TileEntityOpenHearthFurnace() {
		super(4, "Open Hearth Furnace");
		furnaceBurnTime = 0;
		currentItemBurnTime = 0;
		furnaceCookTime = 0;
		cookDuration = 600;//default is 200
		random = new Random();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@SideOnly(Side.CLIENT)
	public int getCookProgressScaled(int i) {
		return (furnaceCookTime * i) / cookDuration;
	}

	@SideOnly(Side.CLIENT)
	public int getBurnTimeRemainingScaled(int i) {
		if (currentItemBurnTime == 0) {
			currentItemBurnTime = cookDuration;
		}
		return (furnaceBurnTime * i) / currentItemBurnTime;
	}

	public boolean isBurning() {
		return furnaceBurnTime > 0;
	}

	@Override
	public void updateEntity() {
		boolean flag = furnaceBurnTime > 0;
		boolean flag1 = false;
		cookDuration = OpenHearthFurnaceRecipes.smelting().getCookTime(this.slots[0], this.slots[1]);
		if (furnaceBurnTime > 0) {
			furnaceBurnTime--;
		}
		if (!worldObj.isRemote) {
			if (furnaceBurnTime == 0 && canSmelt()) {
				if (this.slots[2] != null) {
					currentItemBurnTime = furnaceBurnTime = getItemBurnTime(this.slots[2]);
					if (furnaceBurnTime > 0) {
						flag1 = true;
						if (this.slots[2].getItem().hasContainerItem()) {
							this.slots[2] = new ItemStack(this.slots[2].getItem().getContainerItem());
						}
						else {
							this.slots[2].stackSize--;
						}
						if (this.slots[2].stackSize == 0) {
							this.slots[2] = null;
						}
					}
				}
			}
			if (isBurning() && canSmelt()) {
				furnaceCookTime++;
				if (furnaceCookTime == cookDuration) {
					furnaceCookTime = 0;
					smeltItem();
					flag1 = true;
				}
			}
			else {
				furnaceCookTime = 0;
			}
			if (flag != (furnaceBurnTime > 0)) {
				flag1 = true;
				BlockOpenHearthFurnace.updateHearthFurnaceBlockState(furnaceBurnTime > 0, worldObj, xCoord, yCoord, zCoord, random);
			}
			this.syncTileEntity();
		}
		if (this.worldObj.isRemote) {
			if (furnaceBurnTime > 0) {
				smoke(worldObj, xCoord, yCoord, zCoord, random);
			}
		}
		if (flag1) {
			markDirty();
		}
	}

	@SideOnly(Side.CLIENT)
	private void smoke(World world, int i, int j, int k, Random random) {
		ForgeDirection side = this.getFacing();
		float var7 = (float) i + 0.5F;
		float var8 = (float) j + 0.0F + random.nextFloat() * 6.0F / 16.0F;
		float var9 = (float) k + 0.5F;
		float var10 = 0.52F;
		float var11 = random.nextFloat() * 0.6F - 0.3F;
		float f3 = 0.009F;
		double gaussian = random.nextGaussian() * f3;
		for (int t = 0; t < 50; t++) {
			world.spawnParticle("smoke", var7, (double) j + 1.2F, var9, (double) gaussian, (double) gaussian * 0.002F, (double) gaussian);
		}
		world.spawnParticle("flame", var7, (double) j + 1.03F, var9, 0, 0, 0);
		world.spawnParticle("flame", var7 + 0.06, (double) j + 1.03F, var9 + 0.06, 0, 0, 0);
		world.spawnParticle("flame", var7 - 0.06, (double) j + 1.03F, var9 - 0.06, 0, 0, 0);
		world.spawnParticle("flame", var7 + 0.06, (double) j + 1.03F, var9 - 0.06, 0, 0, 0);
		world.spawnParticle("flame", var7 - 0.06, (double) j + 1.03F, var9 + 0.06, 0, 0, 0);
	}

	private boolean canSmelt(){
		if(this.slots[0] == null){
			return false;
		}
		if(this.slots[1] == null){//second input slot
			return false;
		}
		ItemStack itemstack = OpenHearthFurnaceRecipes.smelting().getSmeltingResultFromItem1(Item.getIdFromItem(this.slots[0].getItem()));
		//second input slot
		return OpenHearthFurnaceRecipes.smelting().areItemPartOfRecipe(this.slots[0].copy(), this.slots[1].copy()) && (this.slots[3] == null || this.slots[3].isItemEqual(itemstack) && this.slots[3].stackSize < itemstack.getMaxStackSize());
	}

	public void smeltItem() {
		if (!canSmelt()) {
			return;
		}
		ItemStack itemstack = OpenHearthFurnaceRecipes.smelting().getSmeltingResultFromItem1(Item.getIdFromItem(this.slots[0].getItem()));
		if (this.slots[3] == null) {
			this.slots[3] = itemstack.copy();

		}
		else if (this.slots[3].getItem() == itemstack.getItem()) {
			this.slots[3].stackSize += itemstack.stackSize;

		}
		if (this.slots[0].getItem().hasContainerItem()) {
			this.slots[0] = new ItemStack(this.slots[0].getItem().getContainerItem());
		}
		else {
			this.slots[0].stackSize--;
		}
		if (this.slots[0].stackSize <= 0) {
			this.slots[0] = null;
		}

		if (this.slots[1].getItem().hasContainerItem()) {
			this.slots[1] = new ItemStack(this.slots[1].getItem().getContainerItem());
		}
		else {
			this.slots[1].stackSize--;
		}
		if (this.slots[1].stackSize <= 0) {
			this.slots[1] = null;
		}
	}

	private int getItemBurnTime(ItemStack it) {
		if (it == null) {
			return 0;
		}
		Item var1 = it.getItem();
		if (Item.getIdFromItem(var1) < 256 && Block.getBlockFromItem(var1).getMaterial() == Material.wood)
			return 300;
		if (var1 == Items.stick)
			return 100;
		if (var1 == Items.coal)
			return 2600;
		if (var1 == Items.lava_bucket)
			return 20000;
		if (var1 == Item.getItemFromBlock(Blocks.sapling))
			return 100;
		if (var1 == Items.blaze_rod)
			return 2500;
		if (var1 == Item.getItemFromBlock(BlockIDs.oreTC.block) && it.getItemDamage() == 1)
			return 2500;
		if (var1 == Item.getItemFromBlock(BlockIDs.oreTC.block) && it.getItemDamage() == 2)
			return 2500;
		if (var1 == ItemIDs.diesel.item)
			return 4000;
		if (var1 == ItemIDs.refinedFuel.item)
			return 6000;
		return GameRegistry.getFuelValue(it);
	}

	public ForgeDirection getFacing() {
		if(facing!=null)return this.facing;
		return ForgeDirection.NORTH;
	}

	public void setFacing(ForgeDirection face) {
		this.facing = face;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt, boolean forSyncing){
		super.writeToNBT(nbt, forSyncing);
		nbt.setByte("Orientation", (byte) facing.ordinal());
		nbt.setShort("BurnTime", (short) furnaceBurnTime);
		nbt.setShort("CookTime", (short) furnaceCookTime);
		nbt.setShort("ItemBurnTime", (short) currentItemBurnTime);
		return nbt;
	}

	@Override
	public NBTTagCompound readFromNBT(NBTTagCompound nbt, boolean forSyncing){
		super.readFromNBT(nbt, forSyncing);
		facing = ForgeDirection.getOrientation(nbt.getByte("Orientation"));
		furnaceBurnTime = nbt.getShort("BurnTime");
		furnaceCookTime = nbt.getShort("CookTime");
		currentItemBurnTime = nbt.getShort("ItemBurnTime");
		return nbt;
	}
}