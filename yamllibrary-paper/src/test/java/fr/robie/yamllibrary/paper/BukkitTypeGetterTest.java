package fr.robie.yamllibrary.paper;

import fr.robie.yamllibrary.InvalidConfigurationException;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bukkit Type Getter Tests")
class BukkitTypeGetterTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static PaperYamlConfiguration roundTrip(PaperYamlConfiguration written)
            throws InvalidConfigurationException {
        PaperYamlConfiguration read = new PaperYamlConfiguration();
        read.loadFromString(written.saveToString());
        return read;
    }

    @Test
    @DisplayName("Should round-trip a PotionEffect")
    void testPotionEffect() throws InvalidConfigurationException {
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 200, 1);

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("buff", effect);

        assertEquals(effect, roundTrip(written).getPotionEffect("buff"));
    }

    @Test
    @DisplayName("Should round-trip a list of PotionEffects")
    void testPotionEffectList() throws InvalidConfigurationException {
        List<PotionEffect> effects = List.of(
                new PotionEffect(PotionEffectType.SPEED, 200, 1),
                new PotionEffect(PotionEffectType.STRENGTH, 100, 0));

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("buffs", effects);

        assertEquals(effects, roundTrip(written).getPotionEffectList("buffs"));
    }

    @Test
    @DisplayName("Should round-trip a FireworkEffect")
    void testFireworkEffect() throws InvalidConfigurationException {
        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.RED)
                .withFlicker()
                .build();

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("firework", effect);

        assertEquals(effect, roundTrip(written).getFireworkEffect("firework"));
    }

    @Test
    @DisplayName("Should round-trip a BoundingBox")
    void testBoundingBox() throws InvalidConfigurationException {
        BoundingBox box = new BoundingBox(0, 0, 0, 16, 64, 16);

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("region", box);

        assertEquals(box, roundTrip(written).getBoundingBox("region"));
    }

    @Test
    @DisplayName("Should fall back for a missing BoundingBox")
    void testBoundingBoxDefault() {
        BoundingBox def = new BoundingBox(0, 0, 0, 1, 1, 1);

        assertEquals(def, new PaperYamlConfiguration().getBoundingBox("absent", def));
    }

    @Test
    @DisplayName("Should round-trip an AttributeModifier")
    void testAttributeModifier() throws InvalidConfigurationException {
        AttributeModifier modifier = new AttributeModifier(
                org.bukkit.NamespacedKey.minecraft("test_bonus"),
                2.5,
                AttributeModifier.Operation.ADD_NUMBER);

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("bonus", modifier);

        assertEquals(modifier, roundTrip(written).getAttributeModifier("bonus"));
    }

    @Test
    @DisplayName("Should round-trip a banner Pattern")
    void testPattern() throws InvalidConfigurationException {
        Pattern pattern = new Pattern(DyeColor.RED, PatternType.STRIPE_TOP);

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("banner", pattern);

        assertEquals(pattern, roundTrip(written).getPattern("banner"));
    }

    @Test
    @DisplayName("Should round-trip a list of banner Patterns")
    void testPatternList() throws InvalidConfigurationException {
        List<Pattern> patterns = List.of(
                new Pattern(DyeColor.RED, PatternType.STRIPE_TOP),
                new Pattern(DyeColor.BLUE, PatternType.STRIPE_BOTTOM));

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("banner", patterns);

        assertEquals(patterns, roundTrip(written).getPatternList("banner"));
    }

    @Test
    @DisplayName("Should reach the new getters from a nested section")
    void testNested() throws InvalidConfigurationException {
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 200, 1);

        PaperYamlConfiguration written = new PaperYamlConfiguration();
        written.set("kits.knight.buff", effect);

        assertEquals(effect, roundTrip(written).getPaperSection("kits.knight").getPotionEffect("buff"));
    }

    @Test
    @DisplayName("Should return null for an absent value")
    void testAbsent() {
        PaperYamlConfiguration config = new PaperYamlConfiguration();

        assertNull(config.getPotionEffect("absent"));
        assertNull(config.getFireworkEffect("absent"));
        assertNull(config.getBoundingBox("absent"));
        assertNull(config.getAttributeModifier("absent"));
        assertNull(config.getPattern("absent"));
        assertTrue(config.getPotionEffectList("absent").isEmpty());
        assertTrue(config.getPatternList("absent").isEmpty());
        assertTrue(config.getAttributeModifierList("absent").isEmpty());
        assertTrue(config.getFireworkEffectList("absent").isEmpty());
    }

    @Test
    @DisplayName("Should stay wire-compatible with vanilla Bukkit for these types")
    void testBukkitWireCompatibility() throws Exception {
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 200, 1);

        PaperYamlConfiguration ours = new PaperYamlConfiguration();
        ours.set("buff", effect);

        org.bukkit.configuration.file.YamlConfiguration bukkit =
                new org.bukkit.configuration.file.YamlConfiguration();
        bukkit.loadFromString(ours.saveToString());

        assertEquals(effect, bukkit.get("buff"));
    }
}
