package com.temporosscoach;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;

public class TemporossState
{
	private static final int INVENTORY_CAPACITY = 28;

	private int rawHarpoonfish;
	private int cookedHarpoonfish;
	private int emptyBuckets;
	private int waterBuckets;
	private int rope;
	private int hammer;
	private int runePouches;
	private int astralRunes;
	private int waterRunes;
	private int fireRunes;
	private int harpoons;
	private int freeInventorySlots;
	private boolean inEncounter;
	private Integer energyPercent;
	private Integer essencePercent;
	private Integer stormIntensityPercent;
	private boolean spiritPoolActive;
	private boolean tethered;
	private boolean doubleFishingSpotActive;
	private boolean shipFireActive;
	private boolean loadingFish;

	void updateInventory(ItemContainer inventory)
	{
		clearInventory();
		if (inventory == null)
		{
			return;
		}
		Item[] items = inventory.getItems();
		int occupiedSlots = 0;
		for (Item item : items)
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			occupiedSlots++;
			int quantity = Math.max(0, item.getQuantity());
			switch (item.getId())
			{
				case ItemID.TEMPOROSS_RAW_HARPOONFISH:
					rawHarpoonfish += quantity;
					break;
				case ItemID.TEMPOROSS_HARPOONFISH:
					cookedHarpoonfish += quantity;
					break;
				case ItemID.BUCKET_EMPTY:
					emptyBuckets += quantity;
					break;
				case ItemID.BUCKET_WATER:
					waterBuckets += quantity;
					break;
				case ItemID.ROPE:
					rope += quantity;
					break;
				case ItemID.HAMMER:
					hammer += quantity;
					break;
				case ItemID.ASTRALRUNE:
					astralRunes += quantity;
					break;
				case ItemID.WATERRUNE:
					waterRunes += quantity;
					break;
				case ItemID.FIRERUNE:
					fireRunes += quantity;
					break;
				case ItemID.BH_RUNE_POUCH:
				case ItemID.BH_RUNE_POUCH_TROUVER:
				case ItemID.DIVINE_RUNE_POUCH:
				case ItemID.DIVINE_RUNE_POUCH_TROUVER:
					runePouches += quantity;
					break;
				case ItemID.HARPOON:
				case ItemID.HUNTING_BARBED_HARPOON:
				case ItemID.DRAGON_HARPOON:
				case ItemID.INFERNAL_HARPOON:
				case ItemID.INFERNAL_HARPOON_EMPTY:
				case ItemID.CRYSTAL_HARPOON:
				case ItemID.CRYSTAL_HARPOON_INACTIVE:
					harpoons += quantity;
					break;
				default:
					break;
			}
		}

		freeInventorySlots = Math.max(0, INVENTORY_CAPACITY - occupiedSlots);
	}

	void clearInventory()
	{
		rawHarpoonfish = 0;
		cookedHarpoonfish = 0;
		emptyBuckets = 0;
		waterBuckets = 0;
		rope = 0;
		hammer = 0;
		runePouches = 0;
		astralRunes = 0;
		waterRunes = 0;
		fireRunes = 0;
		harpoons = 0;
		freeInventorySlots = 0;
	}

	void setHarpoonPresent(boolean harpoonPresent)
	{
		harpoons = harpoonPresent ? 1 : 0;
	}

	void addRunePouchContents(int astralRunes, int waterRunes, int fireRunes)
	{
		this.astralRunes += Math.max(0, astralRunes);
		this.waterRunes += Math.max(0, waterRunes);
		this.fireRunes += Math.max(0, fireRunes);
	}

	void setEncounterState(
		boolean inEncounter,
		Integer energyPercent,
		Integer essencePercent,
		Integer stormIntensityPercent)
	{
		this.inEncounter = inEncounter;
		this.energyPercent = energyPercent;
		this.essencePercent = essencePercent;
		this.stormIntensityPercent = stormIntensityPercent;
	}

	void clearEncounterState()
	{
		setEncounterState(false, null, null, null);
		spiritPoolActive = false;
		tethered = false;
		doubleFishingSpotActive = false;
		shipFireActive = false;
		loadingFish = false;
	}

	void setSpiritPoolActive(boolean spiritPoolActive)
	{
		this.spiritPoolActive = spiritPoolActive;
	}

	void setTethered(boolean tethered)
	{
		this.tethered = tethered;
	}

	void setDoubleFishingSpotActive(boolean doubleFishingSpotActive)
	{
		this.doubleFishingSpotActive = doubleFishingSpotActive;
	}

	void setShipFireActive(boolean shipFireActive)
	{
		this.shipFireActive = shipFireActive;
	}

	void setLoadingFish(boolean loadingFish)
	{
		this.loadingFish = loadingFish;
	}

	void clear()
	{
		clearInventory();
		clearEncounterState();
	}

	public int getRawHarpoonfish()
	{
		return rawHarpoonfish;
	}

	public int getCookedHarpoonfish()
	{
		return cookedHarpoonfish;
	}

	public int getBuckets()
	{
		return emptyBuckets + waterBuckets;
	}

	public int getEmptyBuckets()
	{
		return emptyBuckets;
	}

	public int getWaterBuckets()
	{
		return waterBuckets;
	}

	public boolean isWaterBucketRatioLow()
	{
		return getBuckets() > 0 && waterBuckets * 3 <= getBuckets();
	}

	public int getRope()
	{
		return rope;
	}

	public int getHammer()
	{
		return hammer;
	}

	public int getRunePouches()
	{
		return runePouches;
	}

	public boolean hasHumidifyRunes()
	{
		return astralRunes >= 1 && waterRunes >= 3 && fireRunes >= 1;
	}

	public int getHarpoons()
	{
		return harpoons;
	}

	public int getFreeInventorySlots()
	{
		return freeInventorySlots;
	}

	public boolean isInEncounter()
	{
		return inEncounter;
	}

	public Integer getEnergyPercent()
	{
		return energyPercent;
	}

	public Integer getEssencePercent()
	{
		return essencePercent;
	}

	public Integer getStormIntensityPercent()
	{
		return stormIntensityPercent;
	}

	public boolean isSpiritPoolActive()
	{
		return spiritPoolActive;
	}

	public boolean isTethered()
	{
		return tethered;
	}

	public boolean isDoubleFishingSpotActive()
	{
		return doubleFishingSpotActive;
	}

	public boolean isShipFireActive()
	{
		return shipFireActive;
	}

	public boolean isLoadingFish()
	{
		return loadingFish;
	}
}
