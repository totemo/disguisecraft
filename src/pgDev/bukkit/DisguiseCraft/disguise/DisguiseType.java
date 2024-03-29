package pgDev.bukkit.DisguiseCraft.disguise;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import net.minecraft.server.v1_6_R2.DataWatcher;
import net.minecraft.server.v1_6_R2.Entity;
import net.minecraft.server.v1_6_R2.WatchableObject;
import net.minecraft.server.v1_6_R2.World;

import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.entity.Animals;

import pgDev.bukkit.DisguiseCraft.*;

/**
 * This is the list of possible disguises listed by
 * their Bukkit class name.
 * @author PG Dev Team (Devil Boy)
 */
public enum DisguiseType {
	//Player
	Player(0),
	
	// Mobs
	Bat(65),
	Blaze(61),
	CaveSpider(59),
	Chicken(93),
	Cow(92),
	Creeper(50),
	EnderDragon(63),
	Enderman(58),
	Ghast(56),
	Giant(53),
	Horse(100),
	IronGolem(99),
	MagmaCube(62),
	MushroomCow(96),
	Ocelot(98),
	Pig(90),
	PigZombie(57),
	Sheep(91),
	Silverfish(60),
	Skeleton(51),
	Slime(55),
	Snowman(97),
	Spider(52),
	Squid(94),
	Villager(120),
	Witch(66),
	Wither(64),
	Wolf(95),
	Zombie(54),
	
	// Vehicles
	Boat(1),
	Minecart(10),
	//PoweredMinecart(12),
	//StorageMinecart(11),
	
	//Blocks
	EnderCrystal(51),
	FallingBlock(70),
	TNTPrimed(50);
	
	/**
	 * Entities that are listed in the DisguiseCraft database, but not in
	 * the current Minecraft server version
	 */
	public static LinkedList<DisguiseType> missingDisguises = new LinkedList<DisguiseType>();
	protected static HashMap<Byte, DataWatcher> modelData = new HashMap<Byte, DataWatcher>();
	
	public static Field mapField;
	public static Field boolField;
	
	public static void getDataWatchers(org.bukkit.World world) {
		// Get model datawatchers
    	try {
    		Field watcherField = Entity.class.getDeclaredField("datawatcher");
    		watcherField.setAccessible(true);
    		
			for (DisguiseType m : values()) {
				if (m.isMob()) {
					String mobClass = DynamicClassFunctions.nmsPackage + ".Entity" + m.name();
					if (m == DisguiseType.Giant) {
	    				mobClass = mobClass + "Zombie";
	    			}

	        		try {
	        			Object ent = Class.forName(mobClass).getConstructor(World.class).newInstance(((CraftWorld) world).getHandle());
	        			modelData.put(m.id, (DataWatcher) watcherField.get(ent));
	        		} catch (Exception e) {
	        			missingDisguises.add(m);
	        		}
				}
        	}
		} catch (Exception e) {
			DisguiseCraft.logger.log(Level.SEVERE, "Could not access datawatchers!");
		}
    	
    	// Store important fields
    	int searchingFor = 0; // 0 = Map, 1 = boolean
		for (Field f : DataWatcher.class.getDeclaredFields()) {
			f.setAccessible(true);
			if (searchingFor == 0) {
				if (f.getType() == Map.class) {
					try {
						mapField = f;
					} catch (Exception e) {
						DisguiseCraft.logger.log(Level.SEVERE, "Could not find the DataWatcher Map");
					}
					searchingFor++;
				}
			} else if (searchingFor == 1) {
				if (f.getType() == boolean.class) {
					try {
						boolField = f;
					} catch (Exception e) {
						DisguiseCraft.logger.log(Level.SEVERE, "Could not find the DataWatcher boolean!");
					}
					searchingFor++;
				}
			}
		}
	}
	
	/**
	 * The entity-type ID.
	 */
	public final byte id;
	
	DisguiseType(int i) {
		id = (byte) i;
	}
	
	/**
	 * Check if the mob type is a subclass of an Entity class from Bukkit.
	 * This is extremely useful to seeing if a mob can have a certain
	 * subtype. For example: only members of the Animal class (and villagers)
	 * can have a baby form.
	 * @param cls The class to compare to
	 * @return true if the disguisetype is a subclass, false otherwise
	 */
	public boolean isSubclass(Class<?> cls) {
		try {
			return cls.isAssignableFrom(Class.forName("org.bukkit.entity." + name()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Check if this is a humanoid.
	 * @return true if the type is of a humanoid (player, skeleton, zombie, pigzombie), false otherwise
	 */
	public boolean isHumanoid() {
		return isPlayer() || this == Skeleton || this == Zombie || this == PigZombie;
	}
	
	/**
	 * Check if this is a player.
	 * @return true if the type is of a player, false otherwise
	 */
	public boolean isPlayer() {
		return this == Player;
	}
	
	/**
	 * Check if this is a mob.
	 * @return true if the type is of a mob, false otherwise
	 */
	public boolean isMob() {
		//return this != Player && isSubclass(LivingEntity.class);
		return !isPlayer() && !isObject();
	}
	
	/**
	 * Check if this is an object.
	 * @return true if the type is of an object, false otherwise
	 */
	public boolean isObject() {
		return isVehicle() || isBlock();
	}
	
	/**
	 * Check if this is a vehicle.
	 * @return true if the type is of a vehicle, false otherwise
	 */
	public boolean isVehicle() {
		//return this.isSubclass(Vehicle.class) && this != Pig;
		return this == Boat || this == Minecart;
	}
	
	/**
	 * Check if this is a block.
	 * @return true if the type is of a block, false otherwise
	 */
	public boolean isBlock() {
		return 	this == EnderCrystal || this == FallingBlock || this == TNTPrimed;
	}
	
	/**
	 * Checks if this disguise has a baby form.
	 * @return true if it can be a baby, false otherwise
	 */
	public boolean canBeBaby() {
		return isSubclass(Animals.class) || this == Villager
				|| this == Zombie || this == PigZombie;
	}
	
	/**
	 * Get the DisguiseType from its name
	 * Works like valueOf, but not case sensitive
	 * @param text The string to match with a DisguiseType
	 * @return The DisguiseType with the given name (null if none are found)
	 */
	public static DisguiseType fromString(String text) {
		for (DisguiseType m : DisguiseType.values()) {
			if (text.equalsIgnoreCase(m.name())) {
				if (missingDisguises.contains(m)) {
					return null;
				} else {
					return m;
				}
			}
		}
		return null;
	}
	
	public DataWatcher newMetadata() {
		if (modelData.containsKey(id)) {
			return copyDataWatcher(modelData.get(id));
		} else {
			try {
				return new DataWatcher();
			} catch (Exception e) {
				DisguiseCraft.logger.log(Level.SEVERE, "Could not construct a new DataWatcher", e);
				return null;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static DataWatcher copyDataWatcher(DataWatcher dw) {
		DataWatcher w = new DataWatcher();
		
		// Clone Map
		try {
			HashMap<Integer, WatchableObject> modelMap = ((HashMap<Integer, WatchableObject>) mapField.get(dw));
			HashMap<Integer, WatchableObject> newMap = ((HashMap<Integer, WatchableObject>) mapField.get(w));
			for (Integer index : modelMap.keySet()) {
				newMap.put(index, copyWatchable(modelMap.get(index)));
			}
		} catch (Exception e) {
			DisguiseCraft.logger.log(Level.SEVERE, "Could not clone map in a datawatcher!");
		}
		
		// Clone boolean
		try {
			boolField.setBoolean(w, boolField.getBoolean(dw));
		} catch (Exception e) {
			DisguiseCraft.logger.log(Level.SEVERE, "Could not clone boolean in a datawatcher!");
		}
		
		return w;
	}
	
	private static WatchableObject copyWatchable(WatchableObject watchable) {
		try {
			return new WatchableObject(watchable.c(), watchable.a(), watchable.b());
		} catch (Exception e) {
			DisguiseCraft.logger.log(Level.SEVERE, "Could not copy a WatchableObject", e);
			return null;
		}
	}
	
	/**
	 * Returns the type ID of a minecart type
	 * @param type The type of minecart (chest, furnace, hopper, etc.)
	 * @return The code-side ID or -1 if String not identified
	 */
	public static int getMinecartTypeID(String type) {
		int output = -1;
		if (type.equalsIgnoreCase("chest") || type.equalsIgnoreCase("storage")) {
			output = 1;
		} else if (type.equalsIgnoreCase("furnace") || type.equalsIgnoreCase("powered")) {
			output = 2;
		} else if (type.equalsIgnoreCase("tnt")) {
			output = 3;
		} else if (type.equalsIgnoreCase("mobspawner") || type.equalsIgnoreCase("spawner")) {
			output = 4;
		} else if (type.equalsIgnoreCase("hopper")) {
			output = 5;
		}
		return output;
	}
	
	/**
	 * Just a string containing the possible subtypes. This is mainly
	 * used for plugin help output.
	 */
	public static String subTypes = "baby, black, blue, brown, cyan, " +
		"gray, green, lightblue, lime, magenta, orange, pink, purple, red, " +
		"silver, white, yellow, sheared, charged, tiny, small, big, bigger, massive, godzilla, " +
		"tamed, aggressive, tabby, tuxedo, siamese, burning, saddled, " +
		"librarian, priest, blacksmith, butcher, generic, infected, wither, " +
		"storage, powered, tnt, spawner, hopper, donkey, mule, undead, skeletal";
}