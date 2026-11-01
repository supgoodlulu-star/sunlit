// Harvest-level modifications from 4.1.5A registration/modifyItems.js, isolated for QA.
ItemEvents.modification((e) => {
  ["minecraft:golden_hoe", "minecraft:golden_axe", "minecraft:golden_pickaxe", "minecraft:golden_shovel", "justhammers:gold_reinforced_impact_hammer", "justhammers:gold_hammer", "justhammers:gold_destructor_hammer", "justhammers:gold_impact_hammer", "justhammers:gold_reinforced_hammer"].forEach((tool) => {
    e.modify(tool, (item) => {
      item.digSpeed = 7;
      item.tier = (tier) => { tier.level = 3; };
    });
  });
});
