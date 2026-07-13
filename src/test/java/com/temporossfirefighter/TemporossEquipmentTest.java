package com.temporossfirefighter;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Node;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemporossEquipmentTest
{
	@Test
	public void eitherEquippedImcandoHammerSatisfiesHammerRequirement()
	{
		assertTrue(TemporossFirefighterPlugin.hasEquippedImcandoHammer(equipment(
			new Item(ItemID.IMCANDO_HAMMER, 1))));
		assertTrue(TemporossFirefighterPlugin.hasEquippedImcandoHammer(equipment(
			new Item(ItemID.IMCANDO_HAMMER_OFFHAND, 1))));
	}

	@Test
	public void unrelatedOrMissingEquipmentDoesNotSatisfyHammerRequirement()
	{
		assertFalse(TemporossFirefighterPlugin.hasEquippedImcandoHammer(equipment(
			new Item(ItemID.HAMMER, 1))));
		assertFalse(TemporossFirefighterPlugin.hasEquippedImcandoHammer(null));
	}

	@Test
	public void carriedImcandoHammerVariantsSatisfyHammerRequirement()
	{
		TemporossState state = new TemporossState();
		state.updateInventory(equipment(new Item(ItemID.IMCANDO_HAMMER, 1)));
		assertTrue(state.getHammer() >= 1);

		state.updateInventory(equipment(new Item(ItemID.IMCANDO_HAMMER_OFFHAND, 1)));
		assertTrue(state.getHammer() >= 1);
	}

	@Test
	public void dragonInfernalAndCrystalHarpoonsAreRecognized()
	{
		assertTrue(TemporossFirefighterPlugin.isKnownHarpoonId(ItemID.DRAGON_HARPOON));
		assertTrue(TemporossFirefighterPlugin.isKnownHarpoonId(ItemID.INFERNAL_HARPOON));
		assertTrue(TemporossFirefighterPlugin.isKnownHarpoonId(ItemID.INFERNAL_HARPOON_EMPTY));
		assertTrue(TemporossFirefighterPlugin.isKnownHarpoonId(ItemID.CRYSTAL_HARPOON));
		assertTrue(TemporossFirefighterPlugin.isKnownHarpoonId(ItemID.CRYSTAL_HARPOON_INACTIVE));
	}

	@Test
	public void fullSpiritAnglerSetActsAsRope()
	{
		assertTrue(TemporossFirefighterPlugin.hasFullSpiritAngler(equipment(
			new Item(ItemID.SPIRIT_ANGLER_HAT, 1),
			new Item(ItemID.SPIRIT_ANGLER_TOP, 1),
			new Item(ItemID.SPIRIT_ANGLER_LEGS, 1),
			new Item(ItemID.SPIRIT_ANGLER_BOOTS, 1))));
	}

	@Test
	public void partialSpiritAnglerSetDoesNotActAsRope()
	{
		assertFalse(TemporossFirefighterPlugin.hasFullSpiritAngler(equipment(
			new Item(ItemID.SPIRIT_ANGLER_HAT, 1),
			new Item(ItemID.SPIRIT_ANGLER_TOP, 1),
			new Item(ItemID.SPIRIT_ANGLER_LEGS, 1))));
	}

	@Test
	public void missingEquipmentDoesNotActAsRope()
	{
		assertFalse(TemporossFirefighterPlugin.hasFullSpiritAngler(null));
	}

	private static ItemContainer equipment(Item... items)
	{
		return new ItemContainer()
		{
			@Override
			public int getId()
			{
				return 0;
			}

			@Override
			public Item[] getItems()
			{
				return items;
			}

			@Override
			public Item getItem(int slot)
			{
				return slot >= 0 && slot < items.length ? items[slot] : null;
			}

			@Override
			public boolean contains(int itemId)
			{
				return find(itemId) >= 0;
			}

			@Override
			public int count(int itemId)
			{
				int quantity = 0;
				for (Item item : items)
				{
					if (item.getId() == itemId)
					{
						quantity += item.getQuantity();
					}
				}
				return quantity;
			}

			@Override
			public int size()
			{
				return items.length;
			}

			@Override
			public int count()
			{
				return items.length;
			}

			@Override
			public int find(int itemId)
			{
				for (int slot = 0; slot < items.length; slot++)
				{
					if (items[slot].getId() == itemId)
					{
						return slot;
					}
				}
				return -1;
			}

			@Override
			public Node getNext()
			{
				return null;
			}

			@Override
			public Node getPrevious()
			{
				return null;
			}

			@Override
			public long getHash()
			{
				return 0;
			}
		};
	}
}
