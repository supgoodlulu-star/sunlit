package dev.sunlit.journal;
import java.util.Map;
/** Server-safe labels for verified pack IDs; unknown categories retain their exact IDs. */
final class HallNames {
    private static final Map<String,String> KNOWN=Map.ofEntries(
        Map.entry("society:farming","농사"),Map.entry("society:fishing","낚시"),Map.entry("society:mining","채광"),
        Map.entry("society:cooking","요리"),Map.entry("society:husbandry","목축"),Map.entry("society:combat","전투"),
        Map.entry("society:foraging","채집"),Map.entry("society:woodcutting","벌목"),Map.entry("society:exploration","탐험"),
        Map.entry("society:life","생활"),Map.entry("society:books","서적"));
    static Map<String,String> known(){return KNOWN;}
    static String skill(String id){return KNOWN.getOrDefault(id,id);}
}
