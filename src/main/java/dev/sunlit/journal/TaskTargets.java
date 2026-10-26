package dev.sunlit.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Server-configured, concrete activity targets. Unknown registry entries are never rolled. */
final class TaskTargets {
    record Target(int kind,String subject,int daily,int weekly) {}
    static final List<String> DEFAULTS=List.of(
        "farming|minecraft:wheat|8|32", "farming|minecraft:carrots|8|32", "farming|minecraft:potatoes|8|32",
        "fishing|minecraft:cod|4|16", "fishing|minecraft:salmon|3|12",
        "mining|minecraft:coal_ore|8|32", "mining|minecraft:iron_ore|6|24", "mining|minecraft:copper_ore|8|32",
        "cooking|minecraft:bread|6|24", "cooking|minecraft:baked_potato|6|24", "cooking|minecraft:mushroom_stew|3|12",
        "ranching|minecraft:cow|2|8", "ranching|minecraft:sheep|2|8", "ranching|minecraft:chicken|2|8");
    static Target parse(String entry) {
        String[] p=entry.split("\\|",-1);
        if(p.length!=4)throw new IllegalArgumentException("Expected activity|registry:id|daily|weekly");
        int kind=Arrays.asList(JournalWorld.ACTIVITIES).indexOf(p[0]);
        ResourceLocation id=ResourceLocation.tryParse(p[1]);
        int daily=Integer.parseInt(p[2]),weekly=Integer.parseInt(p[3]);
        if(kind<0||id==null||!p[1].contains(":")||daily<1||daily>100000||weekly<1||weekly>1000000)throw new IllegalArgumentException("Invalid task target");
        return new Target(kind,id.toString(),daily,weekly);
    }
    static boolean valid(Object entry) {
        if(!(entry instanceof String text))return false;
        try{parse(text);return true;}catch(RuntimeException e){return false;}
    }
    private static boolean available(Target t) {
        ResourceLocation id=new ResourceLocation(t.subject());
        return switch(t.kind()) {
            case 0 -> BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.get(id) instanceof net.minecraft.world.level.block.CropBlock;
            case 1 -> BuiltInRegistries.ITEM.containsKey(id) && new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(id)).is(net.minecraft.tags.ItemTags.FISHES);
            case 2 -> BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.get(id).defaultBlockState().is(net.minecraftforge.common.Tags.Blocks.ORES);
            case 3 -> BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id).isEdible();
            case 4 -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
            default -> false;
        };
    }
    static Target choose(int kind) {
        List<Target> pool=new ArrayList<>();
        for(String value:JournalConfig.TASK_TARGETS.get()) {
            try{Target t=parse(value);if(t.kind()==kind&&available(t))pool.add(t);}catch(RuntimeException e){Integrations.failure("taskTarget:"+value,e);}
        }
        if(pool.isEmpty())for(String value:DEFAULTS){Target t=parse(value);if(t.kind()==kind&&available(t))pool.add(t);}
        if(pool.isEmpty())throw new IllegalStateException("No valid task targets for "+kind);
        return pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
    }
    static void assign(CompoundTag task,int period,boolean newTask) {
        Target target=choose(task.getInt("kind"));task.putString("subject",target.subject());
        if(newTask)task.putInt("target",period==0?target.daily():target.weekly());
    }
    static boolean matches(CompoundTag task,int kind,String subject) {
        return task.getInt("kind")==kind && (!task.contains("subject") || task.getString("subject").equals(subject));
    }
}
