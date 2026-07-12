package com.temporosscoach;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Node;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemporossStrategyTest
{
	@Test
	public void requestsPreparationOutsideEncounter()
	{
		TemporossInstruction instruction = new TemporossStrategy()
			.getInstruction(new TemporossState());

		assertEquals("Prepare inventory", instruction.getTitle());
		assertEquals(TemporossInstruction.Type.GENERAL, instruction.getType());
		assertFalse(instruction.getTarget().isPresent());
	}

	@Test
	public void startsFishingInsideEncounter()
	{
		TemporossState state = encounterState();

		TemporossInstruction instruction = new TemporossStrategy().getInstruction(state);

		assertEquals("Catch 17 fish", instruction.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, instruction.getType());
	}

	@Test
	public void missingEncounterSuppliesOverrideRouteInPriorityOrder()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = new TemporossState();
		state.setEncounterState(true, 100, 100, 0);
		state.updateInventory(new TestItemContainer());

		TemporossInstruction rope = strategy.getInstruction(state);
		assertEquals("Get a rope", rope.getTitle());
		assertEquals(TemporossInstruction.Type.GET_ROPE, rope.getType());

		state.updateInventory(new TestItemContainer(new Item(ItemID.ROPE, 1)));
		TemporossInstruction hammer = strategy.getInstruction(state);
		assertEquals("Get a hammer", hammer.getTitle());
		assertEquals(TemporossInstruction.Type.GET_HAMMER, hammer.getType());

		state.updateInventory(new TestItemContainer(
			new Item(ItemID.ROPE, 1), new Item(ItemID.HAMMER, 1)));
		TemporossInstruction buckets = strategy.getInstruction(state);
		assertEquals("Get buckets", buckets.getTitle());
		assertEquals(TemporossInstruction.Type.GET_BUCKETS, buckets.getType());

		state.updateInventory(startingInventory());
		assertEquals("Catch 17 fish", strategy.getInstruction(state).getTitle());
	}

	@Test
	public void doesNotRecheckSuppliesAfterInitialEntryGate()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		assertEquals("Catch 17 fish", strategy.getInstruction(state).getTitle());

		state.updateInventory(new TestItemContainer(new Item(ItemID.TEMPOROSS_RAW_HARPOONFISH, 1)));
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals("Catch 17 fish", instruction.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, instruction.getType());
	}

	@Test
	public void keepsCookingAfterRawFishDropsBelowEight()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);

		setFish(state, 7, 0);
		TemporossInstruction sevenFish = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_FIRST_BATCH, strategy.getStage());
		assertEquals("Catch 17 fish", sevenFish.getTitle());
		assertEquals("7 / 17", sevenFish.getProgressText());

		setFish(state, 8, 0);
		TemporossInstruction cooking = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_FIRST_BATCH, strategy.getStage());
		assertEquals("Cook fish", cooking.getTitle());
		assertEquals("0 / 17", cooking.getProgressText());

		setFish(state, 3, 5);
		TemporossInstruction partlyCooked = strategy.getInstruction(state);
		assertEquals("Cook fish", partlyCooked.getTitle());
		assertEquals("5 / 17", partlyCooked.getProgressText());

		setFish(state, 0, 8);
		TemporossInstruction finishedCooking = strategy.getInstruction(state);
		assertEquals("Catch 17 fish", finishedCooking.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, finishedCooking.getType());
	}

	@Test
	public void firstBatchStopsAtSeventeenCookedWhenRawFishRemain()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);
		setFish(state, 8, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);

		setFish(state, 2, 17);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals(TemporossStage.LOAD_FIRST_BATCH, strategy.getStage());
		assertEquals("Load Fish", instruction.getTitle());
		assertEquals(2, state.getRawHarpoonfish());
	}

	@Test
	public void doesNotCountUnconfirmedFishDecreaseAsLoaded()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);
		setFish(state, 0, 17);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);

		setFish(state, 0, 0);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals(TemporossStage.COLLECT_FIRST_BATCH, strategy.getStage());
		assertEquals("Catch 17 fish", instruction.getTitle());
		assertEquals("0 / 17", instruction.getProgressText());
		assertEquals(17, strategy.getUnexpectedFishLost());
	}

	@Test
	public void countsCookedDecreaseDuringConfirmedLoading()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);
		setFish(state, 0, 17);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);

		state.setLoadingFish(true);
		setFish(state, 0, 12);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals("5 / 17", instruction.getProgressText());
		assertEquals(0, strategy.getUnexpectedFishLost());
	}

	@Test
	public void recoversOnlyMissingOpeningLoadFish()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);
		setFish(state, 0, 17);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);

		state.setLoadingFish(true);
		setFish(state, 0, 2);
		strategy.getInstruction(state);
		state.setLoadingFish(false);
		setFish(state, 0, 0);
		TemporossInstruction recovery = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_FIRST_BATCH, strategy.getStage());
		assertEquals("Catch 2 fish", recovery.getTitle());

		setFish(state, 0, 2);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.COLLECT_SECOND_BATCH);
	}

	@Test
	public void validatesOpeningEnergyWithoutBlockingNextAction()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToSecondBatch(strategy, state);

		assertFalse(strategy.isOpeningLowEnergyConfirmed());
		assertEquals(TemporossInstruction.Type.FISH, strategy.getInstruction(state).getType());
		state.setEncounterState(true, 4, 100, 0);
		assertEquals(TemporossInstruction.Type.FISH, strategy.getInstruction(state).getType());
		assertTrue(strategy.isOpeningLowEnergyConfirmed());
	}

	@Test
	public void earlyOpeningSubmergeInterruptsCollectionUntilPoolCloses()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToSecondBatch(strategy, state);

		state.setEncounterState(true, 0, 100, 0);
		state.setSpiritPoolActive(true);
		TemporossInstruction earlyPool = strategy.getInstruction(state);
		assertEquals("Opening load submerged early", earlyPool.getTitle());
		assertEquals(TemporossInstruction.Type.ATTACK, earlyPool.getType());

		state.setSpiritPoolActive(false);
		state.setEncounterState(true, 100, 100, 0);
		assertEquals(TemporossInstruction.Type.FISH, strategy.getInstruction(state).getType());
	}

	@Test
	public void doubleSpotTemporarilyOverridesInitialCooking()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);

		setFish(state, 8, 0);
		state.setDoubleFishingSpotActive(true);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals(TemporossStage.COLLECT_FIRST_BATCH, strategy.getStage());
		assertEquals("Fish the double spot", instruction.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, instruction.getType());
		assertTrue(instruction.isDoubleFishingSpotPreferred());

		setFish(state, 17, 0);
		TemporossInstruction targetReached = strategy.getInstruction(state);
		assertEquals("Cook fish", targetReached.getTitle());
		assertEquals(TemporossInstruction.Type.COOK, targetReached.getType());
		assertFalse(targetReached.isDoubleFishingSpotPreferred());
	}

	@Test
	public void reportsDoubleSpotStatusWhileFishingToSeventeen()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		strategy.getInstruction(state);
		setFish(state, 8, 0);
		strategy.getInstruction(state);
		setFish(state, 0, 8);

		TemporossInstruction inactive = strategy.getInstruction(state);
		assertEquals("Double spot", inactive.getStatusLabel());
		assertEquals("Inactive", inactive.getStatusText());
		assertEquals(Boolean.FALSE, inactive.getStatusSatisfied());
		assertFalse(inactive.isDoubleFishingSpotPreferred());

		state.setDoubleFishingSpotActive(true);
		TemporossInstruction active = strategy.getInstruction(state);
		assertEquals("Active", active.getStatusText());
		assertEquals(Boolean.TRUE, active.getStatusSatisfied());
		assertTrue(active.isDoubleFishingSpotPreferred());
	}

	@Test
	public void followsGoldenPathThroughMaximumPointsRoute()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();

		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 8, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 0, 8);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 9, 8);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 0, 17);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.COLLECT_SECOND_BATCH);

		setFish(state, 1, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_SECOND_BATCH);
		setFish(state, 19, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_SECOND_BATCH);
		setFish(state, 0, 19);
		assertStage(strategy, state, TemporossStage.LOAD_SECOND_BATCH);
		state.setSpiritPoolActive(true);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.ATTACK_FIRST_POOL);
		state.setSpiritPoolActive(false);
		assertStage(strategy, state, TemporossStage.COLLECT_THIRD_BATCH);
		setFish(state, 1, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_THIRD_BATCH);
		setFish(state, 19, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_THIRD_BATCH);
		setFish(state, 0, 19);
		assertStage(strategy, state, TemporossStage.LOAD_THIRD_BATCH);
		state.setSpiritPoolActive(true);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.ATTACK_SECOND_POOL);
		state.setEncounterState(true, 100, 45, 0);
		assertStage(strategy, state, TemporossStage.ATTACK_SECOND_POOL);
		state.setSpiritPoolActive(false);
		TemporossInstruction finalPreparation = strategy.getInstruction(state);
		assertEquals(TemporossStage.EXTINGUISH_FIRES, strategy.getStage());
		assertEquals("0 / 2", finalPreparation.getProgressText());

		setFish(state, 1, 0);
		TemporossInstruction oneFish = strategy.getInstruction(state);
		assertEquals(TemporossStage.EXTINGUISH_FIRES, strategy.getStage());
		assertEquals("1 / 2", oneFish.getProgressText());
		state.setEncounterState(true, 100, 45, 90);
		assertTrue(strategy.getInstruction(state).getDescription().contains("finish now"));
		state.setEncounterState(true, 100, 45, 0);
		setFish(state, 2, 0);
		TemporossInstruction readyToFinish = strategy.getInstruction(state);
		assertEquals(TemporossStage.EXTINGUISH_FIRES, strategy.getStage());
		assertEquals("Extinguish fires", readyToFinish.getTitle());
		assertEquals(TemporossInstruction.Type.DOUSE_ALL_FIRES, readyToFinish.getType());

		state.setEncounterState(true, 100, 45, 90);
		assertStage(strategy, state, TemporossStage.LOAD_FINAL_FISH);
		state.setSpiritPoolActive(true);
		state.setEncounterState(true, 0, 45, 90);
		setFish(state, 0, 0);
		assertStage(strategy, state, TemporossStage.FINISH_TEMPOROSS);
		assertEquals("Finish Tempoross", strategy.getInstruction(state).getTitle());
	}

	@Test
	public void stormPrioritizesLoadingSecondBatchFishAtNinetyOnePercent()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToSecondBatch(strategy, state);

		state.setEncounterState(true, 4, 100, 90);
		setFish(state, 8, 5);
		TemporossInstruction belowThreshold = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_SECOND_BATCH, strategy.getStage());
		assertEquals(TemporossInstruction.Type.COOK, belowThreshold.getType());

		state.setEncounterState(true, 4, 100, 91);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals(TemporossStage.LOAD_SECOND_BATCH, strategy.getStage());
		assertEquals(TemporossInstruction.Type.LOAD, instruction.getType());
		assertEquals("Load the batch", instruction.getTitle());
	}

	@Test
	public void shipFireInterruptsSecondBatchCollection()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToSecondBatch(strategy, state);

		state.setShipFireActive(true);
		TemporossInstruction instruction = strategy.getInstruction(state);

		assertEquals("Douse ship fire", instruction.getTitle());
		assertEquals(TemporossInstruction.Type.DOUSE_FIRE, instruction.getType());

		state.setShipFireActive(false);
		assertEquals(TemporossInstruction.Type.FISH, strategy.getInstruction(state).getType());
	}

	@Test
	public void recoversExactDeficitAfterPartialSecondBatchLoad()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToRecoveryPool(strategy, state, 10);

		state.setEncounterState(true, 100, 60, 0);
		TemporossInstruction stopFishing = strategy.getInstruction(state);
		assertEquals("Let energy regenerate", stopFishing.getTitle());
		assertEquals(TemporossInstruction.Type.SAFETY, stopFishing.getType());

		state.setSpiritPoolActive(false);
		TemporossInstruction recover = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_RECOVERY_BATCH, strategy.getStage());
		assertEquals("Catch 9 fish", recover.getTitle());

		setFish(state, 9, 0);
		assertEquals(TemporossInstruction.Type.COOK, strategy.getInstruction(state).getType());
		setFish(state, 0, 9);
		assertStage(strategy, state, TemporossStage.LOAD_RECOVERY_BATCH);

		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.COLLECT_THIRD_BATCH);
		setFish(state, 19, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_THIRD_BATCH);
		setFish(state, 0, 19);
		assertStage(strategy, state, TemporossStage.LOAD_THIRD_BATCH);
		state.setSpiritPoolActive(true);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.ATTACK_SECOND_POOL);
	}

	@Test
	public void cooksCarriedRecoveryFishDuringFirstPool()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToRecoveryPool(strategy, state, 10);

		setFish(state, 9, 0);
		TemporossInstruction cooking = strategy.getInstruction(state);
		assertEquals("Cook recovery fish", cooking.getTitle());
		assertEquals(TemporossInstruction.Type.COOK, cooking.getType());

		setFish(state, 0, 9);
		assertEquals("Recovery fish ready", strategy.getInstruction(state).getTitle());
		state.setSpiritPoolActive(false);
		assertStage(strategy, state, TemporossStage.LOAD_RECOVERY_BATCH);
	}

	@Test
	public void lostRecoveryFishReopensCollectionForRemainingDeficit()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToRecoveryPool(strategy, state, 10);
		state.setSpiritPoolActive(false);
		assertStage(strategy, state, TemporossStage.COLLECT_RECOVERY_BATCH);
		setFish(state, 0, 9);
		assertStage(strategy, state, TemporossStage.LOAD_RECOVERY_BATCH);

		setFish(state, 0, 0);
		TemporossInstruction retry = strategy.getInstruction(state);

		assertEquals(TemporossStage.COLLECT_RECOVERY_BATCH, strategy.getStage());
		assertEquals("Catch 9 fish", retry.getTitle());
		assertEquals(9, strategy.getUnexpectedFishLost());
	}

	@Test
	public void partialWaveLossReopensRecoveryCollectionWithInventoryProgress()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToRecoveryPool(strategy, state, 15);
		state.setSpiritPoolActive(false);
		assertStage(strategy, state, TemporossStage.COLLECT_RECOVERY_BATCH);
		setFish(state, 0, 4);
		assertStage(strategy, state, TemporossStage.LOAD_RECOVERY_BATCH);

		setFish(state, 0, 2);
		TemporossInstruction replacement = strategy.getInstruction(state);

		assertEquals(TemporossStage.COLLECT_RECOVERY_BATCH, strategy.getStage());
		assertEquals(TemporossInstruction.Type.FISH, replacement.getType());
		assertEquals("2 / 4", replacement.getProgressText());
		assertEquals(2, strategy.getUnexpectedFishLost());
	}

	@Test
	public void alternatesCookingAndDoubleSpotFishingDuringNineteenFishBatch()
	{
		TemporossStrategy strategy = new TemporossStrategy();
		TemporossState state = encounterState();
		advanceToSecondBatch(strategy, state);

		setFish(state, 5, 0);
		TemporossInstruction normalFishing = strategy.getInstruction(state);
		assertEquals(TemporossStage.COLLECT_SECOND_BATCH, strategy.getStage());
		assertEquals("Catch 19 fish", normalFishing.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, normalFishing.getType());
		assertFalse(normalFishing.isDoubleFishingSpotPreferred());

		setFish(state, 8, 0);
		TemporossInstruction cooking = strategy.getInstruction(state);
		assertEquals("Cook fish", cooking.getTitle());
		assertEquals(TemporossInstruction.Type.COOK, cooking.getType());

		state.setDoubleFishingSpotActive(true);
		TemporossInstruction fishing = strategy.getInstruction(state);
		assertEquals("Fish the double spot", fishing.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, fishing.getType());
		assertTrue(fishing.isDoubleFishingSpotPreferred());

		state.setDoubleFishingSpotActive(false);
		setFish(state, 0, 5);
		TemporossInstruction fallbackFishing = strategy.getInstruction(state);
		assertEquals("Catch 19 fish", fallbackFishing.getTitle());
		assertEquals(TemporossInstruction.Type.FISH, fallbackFishing.getType());
	}

	@Test
	public void acceptsAtLeastNineteenFreeSlots()
	{
		assertEquals("Ready for Tempoross",
			new TemporossStrategy().getInstruction(preparedState(0)).getTitle());
		assertEquals("Ready for Tempoross",
			new TemporossStrategy().getInstruction(preparedState(6)).getTitle());
	}

	@Test
	public void rejectsFewerThanNineteenFreeSlots()
	{
		TemporossInstruction instruction = new TemporossStrategy()
			.getInstruction(preparedState(7));

		assertEquals("Prepare inventory", instruction.getTitle());
	}

	@Test
	public void ignoresEmptySlotPlaceholdersWhenCountingFreeSlots()
	{
		Item[] items = new Item[28];
		items[0] = new Item(ItemID.ROPE, 1);
		items[1] = new Item(ItemID.HAMMER, 1);
		items[2] = new Item(ItemID.BH_RUNE_POUCH, 1);
		for (int slot = 3; slot < items.length; slot++)
		{
			items[slot] = new Item(0, 0);
		}

		TemporossState state = new TemporossState();
		state.updateInventory(new TestItemContainer(items));
		state.setHarpoonPresent(true);
		state.addRunePouchContents(1, 3, 1);

		assertEquals(25, state.getFreeInventorySlots());
		assertEquals("Ready for Tempoross", new TemporossStrategy().getInstruction(state).getTitle());
	}

	@Test
	public void usesInventoryCapacityWhenContainerBackingArrayIsTruncated()
	{
		TemporossState state = new TemporossState();
		state.updateInventory(new TestItemContainer(
			new Item(ItemID.ROPE, 1),
			new Item(ItemID.HAMMER, 1),
			new Item(ItemID.BH_RUNE_POUCH, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(0, 0),
			new Item(0, 0)));

		assertEquals(22, state.getFreeInventorySlots());
	}

	private static TemporossState preparedState(int fillerItems)
	{
		List<Item> items = new ArrayList<>();
		items.add(new Item(ItemID.ROPE, 1));
		items.add(new Item(ItemID.HAMMER, 1));
		items.add(new Item(ItemID.BH_RUNE_POUCH, 1));
		for (int i = 0; i < fillerItems; i++)
		{
			items.add(new Item(ItemID.BUCKET_EMPTY, 1));
		}

		TemporossState state = new TemporossState();
		state.updateInventory(new TestItemContainer(items.toArray(new Item[0])));
		state.setHarpoonPresent(true);
		state.addRunePouchContents(1, 3, 1);
		return state;
	}

	private static TemporossState encounterState()
	{
		TemporossState state = new TemporossState();
		state.setEncounterState(true, 100, 100, 0);
		setFish(state, 0, 0);
		return state;
	}

	private static void advanceToSecondBatch(TemporossStrategy strategy, TemporossState state)
	{
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 8, 0);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 0, 8);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 9, 8);
		assertStage(strategy, state, TemporossStage.COLLECT_FIRST_BATCH);
		setFish(state, 0, 17);
		assertStage(strategy, state, TemporossStage.LOAD_FIRST_BATCH);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.COLLECT_SECOND_BATCH);
	}

	private static void advanceToRecoveryPool(
		TemporossStrategy strategy,
		TemporossState state,
		int cookedFish)
	{
		advanceToSecondBatch(strategy, state);
		state.setEncounterState(true, 4, 100, 91);
		setFish(state, 0, cookedFish);
		assertStage(strategy, state, TemporossStage.LOAD_SECOND_BATCH);
		state.setSpiritPoolActive(true);
		setFish(state, 0, 0);
		assertLoadedStage(strategy, state, TemporossStage.ATTACK_FIRST_POOL);
	}

	private static void setFish(TemporossState state, int raw, int cooked)
	{
		List<Item> items = new ArrayList<>();
		for (Item item : startingInventory().getItems())
		{
			items.add(item);
		}
		if (raw > 0)
		{
			items.add(new Item(ItemID.TEMPOROSS_RAW_HARPOONFISH, raw));
		}
		if (cooked > 0)
		{
			items.add(new Item(ItemID.TEMPOROSS_HARPOONFISH, cooked));
		}
		state.updateInventory(new TestItemContainer(items.toArray(new Item[0])));
	}

	private static TestItemContainer startingInventory()
	{
		return new TestItemContainer(
			new Item(ItemID.ROPE, 1),
			new Item(ItemID.HAMMER, 1),
			new Item(ItemID.BH_RUNE_POUCH, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1),
			new Item(ItemID.BUCKET_EMPTY, 1));
	}

	private static void assertStage(
		TemporossStrategy strategy,
		TemporossState state,
		TemporossStage expectedStage)
	{
		strategy.getInstruction(state);
		assertEquals(expectedStage, strategy.getStage());
	}

	private static void assertLoadedStage(
		TemporossStrategy strategy,
		TemporossState state,
		TemporossStage expectedStage)
	{
		state.setLoadingFish(true);
		assertStage(strategy, state, expectedStage);
		state.setLoadingFish(false);
	}

	private static final class TestItemContainer implements ItemContainer
	{
		private static final int INVENTORY_SIZE = 28;
		private final Item[] items;

		private TestItemContainer(Item... items)
		{
			this.items = items;
		}

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
			return INVENTORY_SIZE;
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
	}
}
