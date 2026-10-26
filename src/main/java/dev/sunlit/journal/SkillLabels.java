package dev.sunlit.journal;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/** Client-only names from the same synced configuration used by the original skill tree. */
final class SkillLabels {
    private SkillLabels() {}

    private static Object config(String categoryId) {
        try {
            Class<?> type=Class.forName("net.puffish.skillsmod.client.SkillsClientMod");
            Object client=Integrations.no(type,"getInstance");
            // Verified against Skills 0.16.1. Read only; never change the original tree.
            Field field=type.getDeclaredField("categories");
            if(!field.trySetAccessible())return null;
            Object value=field.get(client);
            if(!(value instanceof Map<?,?> categories))return null;
            Object category=categories.get(ResourceLocation.tryParse(categoryId));
            return category==null?null:Integrations.no(category,"getConfig");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) {
            Integrations.failure("skillNames",e);return null;
        }
    }

    private static String title(Object config) {
        if(config==null)return null;
        try {
            Object value=Integrations.no(config,"title");
            if(value instanceof Component component) {
                String text=component.getString();
                // Unresolved translation keys must not appear as names.
                if(!text.isBlank() && !(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && !I18n.exists(t.getKey())))return text;
            }
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) { Integrations.failure("skillTitle",e); }
        return null;
    }

    static String category(String id) {
        String name=title(config(id));
        if(name!=null)return name;
        ResourceLocation resource=ResourceLocation.tryParse(id);
        if(resource!=null) {
            String key="sunlit_journal_core.skill_category."+resource.getNamespace()+"."+resource.getPath().replace('/','.');
            if(I18n.exists(key))return I18n.get(key);
        }
        return I18n.get("sunlit_journal_core.skill_category.unknown");
    }

    static String skill(String categoryId,String skillId,int index) {
        Object category=config(categoryId);
        if(category!=null)try {
            Object value=Integrations.no(category,"skills");
            if(value instanceof Map<?,?> skills) {
                Object skill=skills.get(skillId);
                if(skill!=null) {
                    String definition=(String)Integrations.no(skill,"definitionId");
                    Object found=Integrations.call(category,"getDefinitionById",new Class<?>[]{String.class},definition);
                    if(found instanceof Optional<?> optional) {
                        String name=title(optional.orElse(null));
                        if(name!=null)return name;
                    }
                }
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e) { Integrations.failure("skillDefinition",e); }
        return I18n.get("sunlit_journal_core.skill.unknown",index);
    }
}
