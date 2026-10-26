package dev.sunlit.journal;

import java.util.Locale;

/** Recognize pack category IDs and translated names; unknown categories use a neutral badge. */
final class SkillCategoryIcons {
    private SkillCategoryIcons() {}
    static int select(String id,String name) {
        String path=(id.contains(":")?id.substring(id.indexOf(':')+1):id).toLowerCase(Locale.ROOT);
        String label=name.toLowerCase(Locale.ROOT);
        if(path.contains("explor")||path.contains("adventur")||label.contains("탐험")||label.contains("모험"))return 12;
        if(path.contains("book")||path.contains("read")||path.contains("literat")||label.contains("서적")||label.contains("독서")||label.contains("책"))return 14;
        if(path.contains("lifestyle")||path.equals("life")||path.equals("life_skills")||label.contains("생활"))return 13;
        return switch(path){
            case "farming","agriculture"->0;
            case "fishing"->1;
            case "mining"->2;
            case "cooking"->3;
            case "ranching","husbandry"->4;
            case "trading","commerce"->5;
            default->15;
        };
    }
}
