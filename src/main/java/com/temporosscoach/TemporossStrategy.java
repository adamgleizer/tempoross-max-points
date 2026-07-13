package com.temporosscoach;

public class TemporossStrategy
{
	private static final int REQUIRED_FREE_SLOTS = 19;
	private static final int COOKING_THRESHOLD = 8;
	private static final int FIRST_BATCH_TARGET = 17;
	private static final int FULL_BATCH_TARGET = 19;
	private static final int FINAL_FISH_TARGET = 2;
	private static final int STORM_LEAVE_THRESHOLD = 91;
	private static final int FINAL_WARNING_STORM = 90;
	private static final int ESSENCE_GOAL = 50;

	private TemporossStage stage = TemporossStage.COLLECT_FIRST_BATCH;
	private boolean wasInEncounter;
	private boolean initialSupplyCheckComplete;
	private boolean spiritPoolSeen;
	private boolean recoveringFirstBatch;
	private boolean openingEnergyValidationPending;
	private boolean openingLowEnergyConfirmed;
	private boolean openingEarlySubmerge;
	private boolean openingEarlyPoolSeen;
	private boolean openingShipFireSeen;
	private boolean openingShipFireCleared;
	private boolean recoveryPoolPending;
	private boolean finalBucketSetupComplete;
	private int previousCookedFish;
	private int previousTotalFish;
	private int firstBatchLoaded;
	private int secondBatchLoaded;
	private int secondBatchDeficit;
	private int recoveryBatchLoaded;
	private int thirdBatchLoaded;
	private int finalFishLoaded;
	private int unexpectedFishLost;
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
			updateOpeningShipFireGate(state);
			updateOpeningEnergyValidation(state);
			if (!(openingEarlySubmerge && state.isSpiritPoolActive()))
			{
				advance(state);
			}
			updateOpeningShipFireGate(state);
			updateOpeningEnergyValidation(state);
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
		recoveringFirstBatch = false;
		openingEnergyValidationPending = false;
		openingLowEnergyConfirmed = false;
		openingEarlySubmerge = false;
		openingEarlyPoolSeen = false;
		openingShipFireSeen = false;
		openingShipFireCleared = false;
		recoveryPoolPending = false;
		finalBucketSetupComplete = false;
		previousCookedFish = 0;
		previousTotalFish = 0;
		firstBatchLoaded = 0;
		secondBatchLoaded = 0;
		secondBatchDeficit = 0;
		recoveryBatchLoaded = 0;
		thirdBatchLoaded = 0;
		finalFishLoaded = 0;
		unexpectedFishLost = 0;
		latchedCookingStage = null;
	}

	TemporossStage getStage()
	{
		return stage;
	}

	int getUnexpectedFishLost()
	{
		return unexpectedFishLost;
	}

	boolean isOpeningLowEnergyConfirmed()
	{
		return openingLowEnergyConfirmed;
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
		int cookedLoaded = state.isLoadingFish() ? cookedDecrease : 0;
		int totalFishLoaded = state.isLoadingFish() ? totalFishDecrease : 0;
		if (!state.isLoadingFish())
		{
			unexpectedFishLost += totalFishDecrease;
		}

		switch (stage)
		{
			case COLLECT_FIRST_BATCH:
				if (state.getCookedHarpoonfish() >= remainingFirstBatchFish())
				{
					stage = TemporossStage.LOAD_FIRST_BATCH;
					if (!recoveringFirstBatch)
					{
						firstBatchLoaded = 0;
					}
				}
				break;
			case LOAD_FIRST_BATCH:
				firstBatchLoaded += cookedLoaded;
				if (firstBatchLoaded >= FIRST_BATCH_TARGET)
				{
					recoveringFirstBatch = false;
					openingLowEnergyConfirmed = false;
					openingEnergyValidationPending = true;
					stage = TemporossStage.COLLECT_SECOND_BATCH;
				}
				else if (state.getCookedHarpoonfish() == 0)
				{
					recoveringFirstBatch = true;
					latchedCookingStage = null;
					stage = TemporossStage.COLLECT_FIRST_BATCH;
				}
				break;
			case COLLECT_SECOND_BATCH:
				if (isStormAtLeast(state, STORM_LEAVE_THRESHOLD)
					&& state.getCookedHarpoonfish() > 0)
				{
					openingEnergyValidationPending = false;
					stage = TemporossStage.LOAD_SECOND_BATCH;
					secondBatchLoaded = 0;
				}
				else if (state.getCookedHarpoonfish() >= FULL_BATCH_TARGET)
				{
					openingEnergyValidationPending = false;
					stage = TemporossStage.LOAD_SECOND_BATCH;
					secondBatchLoaded = 0;
				}
				break;
			case LOAD_SECOND_BATCH:
				secondBatchLoaded += cookedLoaded;
				if (secondBatchLoaded >= FULL_BATCH_TARGET
					|| (secondBatchLoaded > 0 && state.getCookedHarpoonfish() == 0))
				{
					secondBatchDeficit = Math.max(0, FULL_BATCH_TARGET - secondBatchLoaded);
					enterPoolStage(TemporossStage.ATTACK_FIRST_POOL, state);
				}
				break;
			case ATTACK_FIRST_POOL:
				updatePoolSeen(state);
				if (spiritPoolSeen && !state.isSpiritPoolActive())
				{
					if (secondBatchDeficit == 0)
					{
						stage = TemporossStage.COLLECT_THIRD_BATCH;
					}
					else if (state.getCookedHarpoonfish() >= remainingRecoveryFish())
					{
						stage = TemporossStage.LOAD_RECOVERY_BATCH;
					}
					else
					{
						stage = TemporossStage.COLLECT_RECOVERY_BATCH;
					}
					spiritPoolSeen = false;
				}
				break;
			case COLLECT_RECOVERY_BATCH:
				if (state.getCookedHarpoonfish() >= remainingRecoveryFish())
				{
					stage = TemporossStage.LOAD_RECOVERY_BATCH;
				}
				break;
			case LOAD_RECOVERY_BATCH:
				recoveryBatchLoaded += cookedLoaded;
				if (recoveryBatchLoaded >= secondBatchDeficit)
				{
					recoveryPoolPending = true;
					stage = state.isSpiritPoolActive()
						? TemporossStage.ATTACK_RECOVERY_POOL
						: TemporossStage.COLLECT_THIRD_BATCH;
				}
				else if (!state.isLoadingFish()
					&& totalFishDecrease > 0
					&& totalFish < remainingRecoveryFish())
				{
					latchedCookingStage = null;
					stage = TemporossStage.COLLECT_RECOVERY_BATCH;
				}
				else if (state.getCookedHarpoonfish() == 0)
				{
					stage = TemporossStage.COLLECT_RECOVERY_BATCH;
				}
				break;
			case ATTACK_RECOVERY_POOL:
				updatePoolSeen(state);
				if (spiritPoolSeen && !state.isSpiritPoolActive())
				{
					recoveryPoolPending = false;
					spiritPoolSeen = false;
					stage = TemporossStage.COLLECT_THIRD_BATCH;
				}
				break;
			case COLLECT_THIRD_BATCH:
				if (recoveryPoolPending
					&& (state.isSpiritPoolActive()
						|| (state.getEnergyPercent() != null && state.getEnergyPercent() <= 0)))
				{
					enterPoolStage(TemporossStage.ATTACK_RECOVERY_POOL, state);
					break;
				}
				if ((state.getCookedHarpoonfish() >= FULL_BATCH_TARGET
					|| isStormAtLeast(state, STORM_LEAVE_THRESHOLD))
					&& state.getCookedHarpoonfish() > 0)
				{
					stage = TemporossStage.LOAD_THIRD_BATCH;
					thirdBatchLoaded = 0;
				}
				break;
			case LOAD_THIRD_BATCH:
				thirdBatchLoaded += cookedLoaded;
				if (thirdBatchLoaded > 0 && state.getCookedHarpoonfish() == 0)
				{
					enterPoolStage(TemporossStage.ATTACK_SECOND_POOL, state);
				}
				break;
			case ATTACK_SECOND_POOL:
				updatePoolSeen(state);
				Integer essence = state.getEssencePercent();
				if ((essence != null && essence < ESSENCE_GOAL)
					|| (spiritPoolSeen && !state.isSpiritPoolActive()))
				{
					stage = TemporossStage.EXTINGUISH_FIRES;
					spiritPoolSeen = false;
				}
				break;
			case EXTINGUISH_FIRES:
				if (!finalBucketSetupComplete && state.getFreeInventorySlots() <= FINAL_FISH_TARGET)
				{
					finalBucketSetupComplete = true;
				}
				if (totalFish >= FINAL_FISH_TARGET
					&& isStormAtLeast(state, FINAL_WARNING_STORM))
				{
					stage = TemporossStage.LOAD_FINAL_FISH;
					finalFishLoaded = 0;
				}
				break;
			case LOAD_FINAL_FISH:
				finalFishLoaded += totalFishLoaded;
				if (finalFishLoaded >= FINAL_FISH_TARGET
					|| (finalFishLoaded > 0 && totalFish == 0)
					|| state.isSpiritPoolActive()
					|| (state.getEnergyPercent() != null && state.getEnergyPercent() <= 0))
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
				int firstBatchRemaining = remainingFirstBatchFish();
				return batchCollectionInstruction(state, firstBatchRemaining,
					recoveringFirstBatch
						? "Recover the " + firstBatchRemaining
							+ " cooked fish still missing from the opening load."
						: "");
			case LOAD_FIRST_BATCH:
				return progress("Load Fish",
					"",
					TemporossInstruction.Type.LOAD,
					"Fish loaded", firstBatchLoaded + " / " + FIRST_BATCH_TARGET,
					firstBatchLoaded >= FIRST_BATCH_TARGET);
			case COLLECT_SECOND_BATCH:
				if (openingEarlySubmerge && state.isSpiritPoolActive())
				{
					return instruction("Opening load submerged early",
						"Limit pool damage and let Energy regenerate before resuming collection.",
						TemporossInstruction.Type.ATTACK);
				}
				if (state.isShipFireActive() && !openingShipFireCleared)
				{
					return instruction("Douse ship fire",
						"Extinguish the active fire on the ship before collecting the next batch.",
						TemporossInstruction.Type.DOUSE_FIRE);
				}
				return batchCollectionInstruction(state, FULL_BATCH_TARGET,
					openingEnergyValidationPending
						? "Continue while the cannons confirm the below-10% Energy setup. "
							+ "Douse the ship fire and any blocking island fires."
						: "Douse the ship fire and any blocking island fires.");
			case LOAD_SECOND_BATCH:
				return progress("Load the batch",
					"Load every cooked fish. A short batch will be carried into the recovery guidance.",
					TemporossInstruction.Type.LOAD, "Fish loaded",
					secondBatchLoaded + " / " + FULL_BATCH_TARGET,
					secondBatchLoaded >= FULL_BATCH_TARGET);
			case ATTACK_FIRST_POOL:
				if (secondBatchDeficit > 0)
				{
					if (state.getRawHarpoonfish() > 0)
					{
						return progress("Cook recovery fish",
							"Cook the raw fish already held before Tempoross resurfaces.",
							TemporossInstruction.Type.COOK, "Raw fish left",
							Integer.toString(state.getRawHarpoonfish()), false);
					}
					if (state.getCookedHarpoonfish() >= remainingRecoveryFish())
					{
						return instruction("Recovery fish ready",
							"The missing cooked fish are ready. Let Energy regenerate, then load them after resurfacing.",
							TemporossInstruction.Type.SAFETY);
					}
					Integer recoveryEssence = state.getEssencePercent();
					if (recoveryEssence != null && recoveryEssence <= 60)
					{
						return instruction("Let energy regenerate",
							"Stop fishing and wait for Tempoross to resurface. "
								+ secondBatchDeficit + " cooked fish still need to be recovered.",
							TemporossInstruction.Type.SAFETY);
					}
					return progress("Limit pool damage",
						"The batch was short by " + secondBatchDeficit
							+ ". Fish toward 60% Essence, then stop and preserve recovery time.",
						TemporossInstruction.Type.ATTACK, "Essence",
						recoveryEssence == null ? "-- / ~60%" : recoveryEssence + "% / ~60%",
						false);
				}
				return instruction("Fish the spirit pool",
					"Damage Tempoross until the pool closes and its energy returns.",
					TemporossInstruction.Type.ATTACK);
			case COLLECT_RECOVERY_BATCH:
				int remainingRecovery = remainingRecoveryFish();
				return recoveryCollectionInstruction(state, remainingRecovery,
					"Recover the " + remainingRecovery
						+ " cooked fish missing from the previous 19-fish load.");
			case LOAD_RECOVERY_BATCH:
				return progress("Load recovery fish",
					"Load only the cooked fish needed to complete the interrupted 19-fish batch.",
					TemporossInstruction.Type.LOAD, "Recovery loaded",
					recoveryBatchLoaded + " / " + secondBatchDeficit,
					recoveryBatchLoaded >= secondBatchDeficit);
			case ATTACK_RECOVERY_POOL:
				return instruction("Fish the spirit pool",
					"Damage Tempoross while the recovery load keeps it submerged.",
					TemporossInstruction.Type.ATTACK);
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
						"Douse 5-6 fires if useful, refill buckets, and drop excess buckets "
							+ "to make room while collecting the next 19 fish."));
			case LOAD_THIRD_BATCH:
				return progress("Load the batch",
					"Load every cooked fish into the ammunition crate.", TemporossInstruction.Type.LOAD,
					"Fish loaded", thirdBatchLoaded + " / " + FULL_BATCH_TARGET,
					thirdBatchLoaded >= FULL_BATCH_TARGET);
			case ATTACK_SECOND_POOL:
				Integer essence = state.getEssencePercent();
				return progress("Damage Tempoross",
					"Bring essence below 50%, then move on to bucket preparation.",
					TemporossInstruction.Type.ATTACK, "Essence",
					essence == null ? "--" : essence + "% / <" + ESSENCE_GOAL + "%",
					essence != null && essence < ESSENCE_GOAL);
			case EXTINGUISH_FIRES:
				if (!finalBucketSetupComplete)
				{
					return progress("Fill inventory with buckets",
						"Take buckets until only two inventory slots remain open for the final fish.",
						TemporossInstruction.Type.FILL_BUCKETS, "Free slots",
						state.getFreeInventorySlots() + " / " + FINAL_FISH_TARGET,
						state.getFreeInventorySlots() <= FINAL_FISH_TARGET);
				}
				boolean finalFishReady = totalFish >= FINAL_FISH_TARGET;
				return progress(finalFishReady ? "Extinguish fires" : "Extinguish fires and catch 2 fish",
					isStormAtLeast(state, FINAL_WARNING_STORM)
						? "Storm is at least 90%: secure the two fish, load them, and finish now."
						: finalFishReady
							? "The final fish are ready. Extinguish as many island fires as possible until the 90% finish warning."
							: "Extinguish island fires while collecting two fish for the final load. Cooking is optional.",
					finalFishReady ? TemporossInstruction.Type.DOUSE_ALL_FIRES : TemporossInstruction.Type.FISH,
					"Total fish", totalFish + " / " + FINAL_FISH_TARGET,
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
			&& state.canCastHumidify()
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

	private int remainingFirstBatchFish()
	{
		return Math.max(1, FIRST_BATCH_TARGET - firstBatchLoaded);
	}

	private void updateOpeningEnergyValidation(TemporossState state)
	{
		if (openingEarlySubmerge)
		{
			if (state.isSpiritPoolActive())
			{
				openingEarlyPoolSeen = true;
			}
			else if (openingEarlyPoolSeen)
			{
				openingEarlySubmerge = false;
				openingEarlyPoolSeen = false;
			}
			return;
		}
		if (!openingEnergyValidationPending)
		{
			return;
		}

		Integer energy = state.getEnergyPercent();
		if (state.isSpiritPoolActive() || (energy != null && energy <= 0))
		{
			openingEnergyValidationPending = false;
			openingEarlySubmerge = true;
			openingEarlyPoolSeen = state.isSpiritPoolActive();
		}
		else if (energy != null && energy < 10)
		{
			openingEnergyValidationPending = false;
			openingLowEnergyConfirmed = true;
		}
	}

	private void updateOpeningShipFireGate(TemporossState state)
	{
		if (stage != TemporossStage.COLLECT_SECOND_BATCH || openingShipFireCleared)
		{
			return;
		}
		if (state.isShipFireActive())
		{
			openingShipFireSeen = true;
		}
		else if (openingShipFireSeen)
		{
			openingShipFireCleared = true;
		}
	}

	private int remainingRecoveryFish()
	{
		return Math.max(0, secondBatchDeficit - recoveryBatchLoaded);
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

	private TemporossInstruction recoveryCollectionInstruction(
		TemporossState state,
		int target,
		String description)
	{
		int totalFish = totalFish(state);
		if (totalFish >= target)
		{
			return progress("Cook recovery fish", description, TemporossInstruction.Type.COOK,
				"Cooked harpoonfish", state.getCookedHarpoonfish() + " / " + target,
				state.getCookedHarpoonfish() >= target);
		}

		boolean doubleSpotActive = state.isDoubleFishingSpotActive();
		return progressWithStatus(
			doubleSpotActive ? "Fish the double spot" : "Catch " + target + " fish",
			description, TemporossInstruction.Type.FISH,
			"Total fish", totalFish + " / " + target, false,
			"Double spot", doubleSpotActive ? "Active" : "Inactive",
			doubleSpotActive, doubleSpotActive);
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
			return "Storm is at least 91%: leave for the ship and load what is cooked now.";
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
