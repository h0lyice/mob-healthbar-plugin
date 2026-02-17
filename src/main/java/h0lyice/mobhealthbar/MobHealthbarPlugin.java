package h0lyice.mobhealthbar;

import java.util.Locale;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class MobHealthbarPlugin extends JavaPlugin implements Listener {
	// Unicode heart symbol: U+2764
	private static final Component HEART_RED = Component.text(" \u2764", NamedTextColor.RED);
	private static final Component HEART_GOLD = Component.text(" \u2764", NamedTextColor.GOLD);

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageByEntityEvent event) {
		Player attacker = resolveAttacker(event);
		if (attacker == null) {
			return;
		}

		Entity target = event.getEntity();
		if (!(target instanceof LivingEntity living)) {
			return;
		}
		if (living instanceof Player) {
			return;
		}

		double finalDamage = event.getFinalDamage();

		double healthBefore = living.getHealth();
		double absorptionBefore = living.getAbsorptionAmount();

		double absorptionAfter = Math.max(0.0, absorptionBefore - finalDamage);
		double overflowDamage = Math.max(0.0, finalDamage - absorptionBefore);
		double healthAfter = Math.max(0.0, healthBefore - overflowDamage);

		AttributeInstance maxHealthAttr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (maxHealthAttr == null) {
			return;
		}

		double maxHealth = maxHealthAttr.getValue();

		Component name = living.customName();
		if (name == null) {
			name = Component.translatable(living.getType().translationKey());
		}

		Component msg = Component.empty()
			.append(name.colorIfAbsent(NamedTextColor.GRAY))
			.append(Component.space())
			.append(Component.text(format1(healthAfter), NamedTextColor.WHITE))
			.append(Component.text(" / ", NamedTextColor.DARK_GRAY))
			.append(Component.text(formatSmart(maxHealth), NamedTextColor.WHITE))
			.append(HEART_RED);

		if (absorptionAfter > 0.01) {
			msg = msg.append(Component.space())
				.append(Component.text("+", NamedTextColor.DARK_GRAY))
				.append(Component.text(formatSmart(absorptionAfter),
						NamedTextColor.GOLD))
				.append(HEART_GOLD);
		}

		Audience audience = attacker;
		audience.sendActionBar(msg);
	}

	private static Player resolveAttacker(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		if (damager instanceof Player p) {
			return p;
		}

		if (damager instanceof Projectile projectile) {
			Object shooter = projectile.getShooter();
			if (shooter instanceof Player p) {
				return p;
			}
		}

		return null;
	}

	private static String format1(double v) {
		return String.format(Locale.US, "%.1f", v);
	}

	private static String formatSmart(double v) {
		double rounded = Math.rint(v);
		if (Math.abs(v - rounded) < 1e-9) {
			return Long.toString((long) rounded);
		}
		return format1(v);
	}
}
