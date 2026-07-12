package com.temporosscoach;

public class TemporossStrategy
{
	private static final int REQUIRED_FREE_SLOTS = 19;
	private static final int COOKING_THRESHOLD = 8;
	private static final int FIRST_BATCH_TARGET = 17;
	private static final int FULL_BATCH_TARGET = 19;
	private static final int FINAL_FISH_TARGET = 2;
	private static final int STORM_LEAVE_THRESHOLD = 91;
	private static final int ESSENCE_GOAL = 50;

	private TemporossStage stage = TemporossStage.COLLECT_FIRST_BATCH;
	private boolean wasInEncounter;
	private boolean initialSupplyCheckComplete;
	private boolean spiritPoolSeen;
	private int previousCookedFish;
	private int previousTotalFish;
	private int firstBatchLoaded;
	private int secondBatchLoaded;
	private int thirdBatchLoaded;
	private int finalFishLoaded;
	private TemporossStage latchedCookingStage;

	public TemporossInstruction getInstruction(TemporossState state)
	{
		if (!state.isInEncounter())
		{
			reset();
			return preparationInstruction(state);
		}

		if (!wasInEncounter)
		{
			beginEncounter(state);
		}

		if (!initialSupplyCheckComplete)
		{
			TemporossInstruction supplyInstruction = missingSupplyInstruction(state);
			if (supplyInstruction != null)
			{
				previousCookedFish = state.getCookedHarpoonfish();
				previousTotalFish = totalFish(state);
				wasInEncounter = true;
				return supplyInstruction;
			}
			initialSupplyCheckComplete = true;
		}

		if (wasInEncounter)
		{
			advance(state);
		}

		TemporossInstruction instruction = instructionFor(state);
		previousCookedFish = state.getCookedHarpoonfish();
		previousTotalFish = totalFish(state);
		wasInEncounter = true;
		return instruction;
	}

	private TemporossInstruction missingSupplyInstruction(TemporossState state)
	{
		if (state.getRope() < 1)
		{
			return progress("Get a rope", "Click the highlighted Ropes object.",
				TemporossInstruction.Type.GET_ROPE, "Rope", state.getRope() + " / 1", false);
		}
		if (state.getHammer() < 1)
		{
			return progress("Get a hammer", "Click the highlighted Hammers object.",
				TemporossInstruction.Type.GET_HAMMER, "Hammer", state.getHammer() + " / 1", false);
		}
		if (stage == TemporossStage.COLLECT_FIRST_BATCH
			&& totalFish(state) == 0
			&& state.getFreeInventorySlots() > REQUIRED_FREE_SLOTS)
		{
			return progress("Get buckets",
				"Click the highlighted Buckets object until 19 inventory slots remain open.",
				TemporossInstruction.Type.GET_BUCKETS, "Free slots",
				state.getFreeInventorySlots() + " / " + REQUIRED_FREE_SLOTS, false);
		}
		return null;
	}

	void reset()
	{
		stage = TemporossStage.COLLECT_FIRST_BATCH;
		wasInEncounter = false;
		initialSupplyCheckComplete = false;
		spiritPoolSeen = false;
		previousCookedFish = 0;
		previousTotalFish = 0;
		firstBatchLoaded = 0;
		secondBatchLoaded = 0;
		thirdBatchLoaded = 0;
		finalFishLoaded = 0;
		latchedCookingStage = null;
	}

	TemporossStage getStage()
	{
		return stage;
	}

	private void beginEncounter(TemporossState state)
	{
		stage = TemporossStage.COLLECT_FIRST_BATCH;
		initialSupplyCheckComplete = false;
		previousCookedFish = state.getCookedHarpoonfish();
		previousTotalFish = totalFish(state);
	}

	private void advance(TemporossState state)
	{
		int totalFish = totalFish(state);
		int cookedDecrease = Math.max(0, previousCookedFish - state.getCookedHarpoonfish());
		int totalFishDecrease = Math.max(0, previousTotalFish - totalFish);

		switch (stage)
		{
			case COLLECT_FIRST_BATCH:
				if (state.getCookedHarpoonfish() >= FIRST_BATCH_TARGET)
				{
					stage = TemporossStage.LOAD_FIRST_BATCH;
					firstBatchLoaded = 0;
				}
				break;
			case LOAD_FIRST_BATCH:
				firstBatchLoaded += cookedDecrease;
				if (firstBatchLoaded >= FIRST_BATCH_TARGET
					|| (firstBatchLoaded > 0 && state.getCookedHarpoonfish() == 0))
				{
					stage = TemporossStage.COLLECT_SECOND_BATCH;
				}
				break;
			case COLLECT_SECOND_BATCH:
				if (isStormAtLeast(state, STORM_LEAVE_THRESHOLD)
					&& state.getCookedHarpoonfish() > 0)
				{
					stage = TemporossStage.LOAD_SECOND_BATCH;
					secondBatchLoaded = 0;
				}
				else if (state.getCookedHarpoonfish() >= FULL_BATCH_TARGET)
				{
					stage = TemporossStage.LOAD_SECOND_BATCH;
					secondBatchLoaded = 0;
				}
				break;
			case LOAD_SECOND_BATCH:
				secondBatchLoaded += cookedDecrease;
				if (secondBatchLoaded > 0 && state.getCookedHarpoonfish() == 0)
				{
					enterPoolStage(TemporossStage.ATTACK_FIRST_POOL, state);
				}
				break;
			case ATTACK_FIRST_POOL:
				updatePoolSeen(state);
				if (spiritPoolSeen && !state.isSpiritPoolActive())
				{
					stage = TemporossStage.PREPARE_SECOND_PHASE;
					spiritPoolSeen = false;
				}
				break;
			case PREPARE_SECOND_PHASE:
				if (isReadyForSecondPhaseFishing(state))
				{
					stage = TemporossStage.COLLECT_THIRD_BATCH;
				}
				break;
			case COLLECT_THIRD_BATCH:
				if ((state.getCookedHarpoonfish() >= FULL_BATCH_TARGET
					|| isStormAtLeast(state, STORM_LEAVE_THRESHOLD))
					&& state.getCookedHarpoonfish() > 0)
				{
					stage = TemporossStage.LOAD_THIRD_BATCH;
					thirdBatchLoaded = 0;
				}
				break;
			case LOAD_THIRD_BATCH:
				thirdBatchLoaded += cookedDecrease;
				if (thirdBatchLoaded > 0 && state.getCookedHarpoonfish() == 0)
				{
					enterPoolStage(TemporossStage.ATTACK_SECOND_POOL, state);
				}
				break;
			case ATTACK_SECOND_POOL:
				updatePoolSeen(state);
				if (spiritPoolSeen && !state.isSpiritPoolActive())
				{
					stage = TemporossStage.EXTINGUISH_FIRES;
					spiritPoolSeen = false;
				}
				break;
			case EXTINGUISH_FIRES:
				if (totalFish > 0)
				{
					stage = TemporossStage.CATCH_FINAL_FISH;
				}
				break;
			case CATCH_FINAL_FISH:
				if (totalFish >= FINAL_FISH_TARGET)
				{
					stage = TemporossStage.LOAD_FINAL_FISH;
					finalFishLoaded = 0;
				}
				break;
			case LOAD_FINAL_FISH:
				finalFishLoaded += totalFishDecrease;
				if (finalFishLoaded >= FINAL_FISH_TARGET
					|| (finalFishLoaded > 0 && totalFish == 0))
				{
					stage = TemporossStage.FINISH_TEMPOROSS;
				}
				break;
			case FINISH_TEMPOROSS:
				break;
		}
	}

	private TemporossInstruction instructionFor(TemporossState state)
	{
		int totalFish = totalFish(state);
		switch (stage)
		{
			case COLLECT_FIRST_BATCH:
				return batchCollectionInstruction(state, FIRST_BATCH_TARGET, "");
			case LOAD_FIRST_BATCH:
				return progress("Load Fish",
					"",
					TemporossInstruction.Type.LOAD,
					"Fish loaded", firstBatchLoaded + " / " + FIRST_BATCH_TARGET,
					firstBatchLoaded >= FIRST_BATCH_TARGET);
			case COLLECT_SECOND_BATCH:
				if (state.isShipFireActive())
				{
					return instruction("Douse ship fire",
						"Extinguish the active fire on the ship before collecting the next batch.",
						TemporossInstruction.Type.DOUSE_FIRE);
				}
				return batchCollectionInstruction(state, FULL_BATCH_TARGET,
					"Douse the ship fire and any blocking island fires.");
			case LOAD_SECOND_BATCH:
				return progress("Load the batch",
					"Load every cooked fish. A short batch will be carried into the recovery guidance.",
					TemporossInstruction.Type.LOAD, "Fish loaded",
					secondBatchLoaded + " / " + FULL_BATCH_TARGET,
					secondBatchLoaded >= FULL_BATCH_TARGET);
			case ATTACK_FIRST_POOL:
				int deficit = Math.max(0, FULL_BATCH_TARGET - secondBatchLoaded);
				return instruction("Fish the spirit pool",
					deficit == 0
						? "Damage Tempoross until the pool closes and its energy returns."
						: "Batch was short by " + deficit + "; limit or skip pool damage to preserve recovery time.",
					TemporossInstruction.Type.ATTACK);
			case PREPARE_SECOND_PHASE:
				return progress("Reset for 19 fish",
					"Douse 5-6 fires if needed, refill buckets, then drop excess buckets to make room for 19 fish.",
					TemporossInstruction.Type.SAFETY, "Ready to fish",
					secondPhasePreparationStatus(state), isReadyForSecondPhaseFishing(state));
			case COLLECT_THIRD_BATCH:
				if (isStormAtLeast(state, STORM_LEAVE_THRESHOLD)
					&& state.getRawHarpoonfish() > 0)
				{
					return progress("Cook fish", stormGuidance(state,
						"Cook what remains and leave for the ammunition crate."),
						TemporossInstruction.Type.COOK, "Cooked harpoonfish",
						state.getCookedHarpoonfish() + " / " + FULL_BATCH_TARGET, false);
				}
				return batchCollectionInstruction(state, FULL_BATCH_TARGET,
					stormGuidance(state,
						"Catch the second 19-fish batch before storm intensity becomes unsafe."));
			case LOAD_THIRD_BATCH:
				return progress("Load the batch",
					"Load every cooked fish into the ammunition crate.", TemporossInstruction.Type.LOAD,
					"Fish loaded", thirdBatchLoaded + " / " + FULL_BATCH_TARGET,
					thirdBatchLoaded >= FULL_BATCH_TARGET);
			case ATTACK_SECOND_POOL:
				Integer essence = state.getEssencePercent();
				return progress("Damage Tempoross",
					"Bring essence below 60%, ideally below 50%, then wait for the pool to close.",
					TemporossInstruction.Type.ATTACK, "Essence",
					essence == null ? "--" : essence + "% / <" + ESSENCE_GOAL + "%",
					essence != null && essence < ESSENCE_GOAL);
			case EXTINGUISH_FIRES:
				return instruction("Extinguish fires",
					"Take buckets from the dock and clear fires around the island. Leave time to finish.",
					TemporossInstruction.Type.SAFETY);
			case CATCH_FINAL_FISH:
				return progress("Catch 2 fish",
					"Catch two fish before storm intensity becomes too high. Cooking is optional.",
					TemporossInstruction.Type.FISH, "Total fish", totalFish + " / " + FINAL_FISH_TARGET,
					totalFish >= FINAL_FISH_TARGET);
			case LOAD_FINAL_FISH:
				return progress("Load 2 fish",
					"Load both fish and avoid running through active fire.", TemporossInstruction.Type.LOAD,
					"Fish loaded", finalFishLoaded + " / " + FINAL_FISH_TARGET,
					finalFishLoaded >= FINAL_FISH_TARGET);
			case FINISH_TEMPOROSS:
				return instruction("Finish Tempoross",
					"Fish at the spirit pool and leave enough time to secure the kill.",
					TemporossInstruction.Type.ATTACK);
			default:
				return placeholderInstruction();
		}
	}

	private TemporossInstruction preparationInstruction(TemporossState state)
	{
		if (hasRequiredInventory(state))
		{
			return instruction("Ready for Tempoross",
				"Required supplies detected.",
				TemporossInstruction.Type.GENERAL);
		}
		return instruction("Prepare inventory",
			"Bring 1 rope, 1 hammer, runes for Humidify, a harpoon, and leave at least 19 free slots.",
			TemporossInstruction.Type.GENERAL);
	}

	private static boolean hasRequiredInventory(TemporossState state)
	{
		return state.getRope() >= 1
			&& state.getHammer() >= 1
			&& state.hasHumidifyRunes()
			&& state.getHarpoons() >= 1
			&& state.getFreeInventorySlots() >= REQUIRED_FREE_SLOTS;
	}

	private void enterPoolStage(TemporossStage poolStage, TemporossState state)
	{
		stage = poolStage;
		spiritPoolSeen = state.isSpiritPoolActive();
	}

	private void updatePoolSeen(TemporossState state)
	{
		if (state.isSpiritPoolActive())
		{
			spiritPoolSeen = true;
		}
	}

	private static int totalFish(TemporossState state)
	{
		return state.getRawHarpoonfish() + state.getCookedHarpoonfish();
	}

	private static boolean isReadyForSecondPhaseFishing(TemporossState state)
	{
		return state.getFreeInventorySlots() >= REQUIRED_FREE_SLOTS
			&& state.getBuckets() > 0
			&& state.getEmptyBuckets() == 0;
	}

	private static String secondPhasePreparationStatus(TemporossState state)
	{
		return state.getFreeInventorySlots() + "/19+ slots, "
			+ state.getWaterBuckets() + "/" + state.getBuckets() + " water";
	}

	private TemporossInstruction batchCollectionInstruction(
		TemporossState state,
		int target,
		String description)
	{
		int totalFish = totalFish(state);
		if (totalFish >= target)
		{
			latchedCookingStage = stage;
			return progress("Cook fish", description, TemporossInstruction.Type.COOK,
				"Cooked harpoonfish", state.getCookedHarpoonfish() + " / " + target,
				state.getCookedHarpoonfish() >= target);
		}
		if (state.isDoubleFishingSpotActive())
		{
			latchedCookingStage = null;
			return progressWithStatus("Fish the double spot", description,
				TemporossInstruction.Type.FISH, "Total fish", totalFish + " / " + target,
				false, "Double spot", "Active", true, true);
		}
		if (state.getRawHarpoonfish() == 0)
		{
			latchedCookingStage = null;
		}
		else if (latchedCookingStage == stage
			|| state.getRawHarpoonfish() >= COOKING_THRESHOLD)
		{
			latchedCookingStage = stage;
			return progress("Cook fish", description, TemporossInstruction.Type.COOK,
				"Cooked harpoonfish", state.getCookedHarpoonfish() + " / " + target, false);
		}

		return progressWithStatus("Catch " + target + " fish", description,
			TemporossInstruction.Type.FISH, "Total fish", totalFish + " / " + target,
			false, "Double spot", "Inactive", false, false);
	}

	private static boolean isStormAtLeast(TemporossState state, int threshold)
	{
		return state.getStormIntensityPercent() != null
			&& state.getStormIntensityPercent() >= threshold;
	}

	private static String stormGuidance(TemporossState state, String normalGuidance)
	{
		if (isStormAtLeast(state, STORM_LEAVE_THRESHOLD))
		{
			return "Storm is at least 93%: leave for the ship and load what is cooked now.";
		}
		return normalGuidance;
	}

	private static TemporossInstruction instruction(
		String title,
		String description,
		TemporossInstruction.Type type)
	{
		return new TemporossInstruction(title, description, null, type);
	}

	private static TemporossInstruction progress(
		String title,
		String description,
		TemporossInstruction.Type type,
		String progressLabel,
		String progressText,
		boolean satisfied)
	{
		return new TemporossInstruction(
			title, description, null, type, progressLabel, progressText, satisfied);
	}

	private static TemporossInstruction progressWithStatus(
		String title,
		String description,
		TemporossInstruction.Type type,
		String progressLabel,
		String progressText,
		boolean progressSatisfied,
		String statusLabel,
		String statusText,
		boolean statusSatisfied,
		boolean doubleFishingSpotPreferred)
	{
		return new TemporossInstruction(title, description, null, type,
			progressLabel, progressText, progressSatisfied,
			statusLabel, statusText, statusSatisfied, doubleFishingSpotPreferred);
	}

	private static TemporossInstruction progress(
		String title,
		TemporossInstruction.Type type,
		String progressLabel,
		String progressText,
		boolean satisfied)
	{
		return new TemporossInstruction(
			title, null, type, progressLabel, progressText, satisfied);
	}

	static TemporossInstruction placeholderInstruction()
	{
		return instruction("Preparing", "Strategy guidance will appear here.",
			TemporossInstruction.Type.GENERAL);
	}
}
