// priority: -1000
// Add after the Society 4.1.5A globals; restart Minecraft/server after installation.
// Keep the original per-slot quality, stages, attributes, sale eligibility and removal logic.
const SunlitFishSizes = Java.loadClass("dev.sunlit.journal.FishSizes");
if (typeof global.processShippingBinInventory !== "function") {
  throw new Error("Sunlit fish sizes: original Society shipping-bin calculator not found");
}
const sunlitOriginalShippingCalculation = global.processShippingBinInventory;
global.processShippingBinInventory = (inventory, inventorySlots, attributes, stages, returnRemoved, simulated) => {
  let total = 0;
  let removed = [];
  for (let i = 0; i < inventorySlots; i++) {
    // Capture the size before the original calculator optionally clears the slot.
    var multiplier = SunlitFishSizes.saleMultiplier(inventory.getStackInSlot(i));
    var oneSlot = {
      getStackInSlot: (slot) => inventory.getStackInSlot(i),
      setStackInSlot: (slot, value) => inventory.setStackInSlot(i, value)
    };
    var result = sunlitOriginalShippingCalculation(oneSlot, 1, attributes, stages, returnRemoved, simulated);
    var value = Number(result.calculatedValue);
    total += multiplier === 1 ? value : Math.round(value * multiplier);
    if (result.removedItems.length > 0) removed.push(i);
  }
  return { calculatedValue: total, removedItems: removed };
};
console.info("[Sunlit] Fish size prices connected to original Society shipping calculator");
